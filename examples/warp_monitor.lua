-- Monitor UI sample for cosmowarp's Warp Drive peripheral.
--
-- Setup: a computer with a Warp Drive and an Advanced Monitor attached (directly, or over wired
-- modems). Advanced Monitor specifically -- the regular one can't report touches, so the list
-- would render but not be clickable.
--
-- This is a starting point meant to be edited, not a finished product. The peripheral exposes
-- data and controls only; how it looks is yours to decide.

local drive = peripheral.find("warp_drive")
if not drive then
  error("No Warp Drive found. Attach one directly or over a wired modem.")
end

local monitor = peripheral.find("monitor")
if not monitor then
  error("No monitor found. Attach an Advanced Monitor (the touch-capable kind).")
end

monitor.setTextScale(0.5)
term.redirect(monitor)

local W, H = term.getSize()

-- Rows on screen that currently correspond to a warp point, so a touch can be mapped back to the
-- point it landed on. Rebuilt on every draw, since the list can change under us at any time
-- (someone swapping the crystal, registering a point from the GUI, etc.).
local rowMap = {}

-- Where the list starts/ends vertically. Leaves room for the header block above and the buttons
-- and status line below.
local LIST_TOP = 6
local LIST_BOTTOM = H - 4

-- Snapshot of everything currently on screen. Each poll builds a fresh one and compares: if
-- nothing visible changed, the draw is skipped entirely. Redrawing unconditionally (clear +
-- repaint, twice a second) is what makes an untouched monitor visibly flicker, since every
-- repaint briefly blanks the screen. While charging, the countdown genuinely changes each poll,
-- so it redraws every time -- same as before, which is what you want there.
local lastState = nil

local function readState()
  local state = {
    energy = drive.getEnergy(),
    capacity = drive.getEnergyCapacity(),
    cost = drive.getWarpCost(),
    hasCrystal = drive.hasCrystal(),
    charging = drive.isCharging(),
    -- Rounded to the same precision the status line prints, so sub-visible jitter in the raw
    -- value doesn't trigger a pointless repaint.
    chargeSeconds = math.floor(drive.getChargeSeconds() * 10) / 10,
    points = {},
  }

  if state.hasCrystal then
    for i, point in ipairs(drive.getWarpPoints()) do
      state.points[i] = {
        name = point.name,
        x = point.x, y = point.y, z = point.z,
        selected = point.selected,
      }
    end
  end

  return state
end

local function sameState(a, b)
  if a == nil or b == nil then return false end
  if a.energy ~= b.energy or a.capacity ~= b.capacity or a.cost ~= b.cost then return false end
  if a.hasCrystal ~= b.hasCrystal or a.charging ~= b.charging then return false end
  if a.chargeSeconds ~= b.chargeSeconds then return false end
  if #a.points ~= #b.points then return false end

  for i = 1, #a.points do
    local p, q = a.points[i], b.points[i]
    if p.name ~= q.name or p.x ~= q.x or p.y ~= q.y or p.z ~= q.z or p.selected ~= q.selected then
      return false
    end
  end

  return true
end

local function drawBar(y, filled, total, width)
  local ratio = total > 0 and math.min(1, filled / total) or 0
  local lit = math.floor(width * ratio + 0.5)

  term.setCursorPos(1, y)
  term.setBackgroundColor(colors.gray)
  term.write(string.rep(" ", width))

  if lit > 0 then
    term.setCursorPos(1, y)
    term.setBackgroundColor(ratio >= 1 and colors.lime or colors.orange)
    term.write(string.rep(" ", lit))
  end

  term.setBackgroundColor(colors.black)
end

local function centered(y, text, color)
  term.setCursorPos(math.max(1, math.floor((W - #text) / 2) + 1), y)
  term.setTextColor(color or colors.white)
  term.write(text)
end

local function drawButton(x, y, label, enabled)
  term.setCursorPos(x, y)
  term.setBackgroundColor(enabled and colors.blue or colors.gray)
  term.setTextColor(enabled and colors.white or colors.lightGray)
  term.write(" " .. label .. " ")
  term.setBackgroundColor(colors.black)
  term.setTextColor(colors.white)
end

-- Returns the x range the Warp button occupies, so the touch handler knows where it is without
-- the two going out of sync.
local function warpButtonBounds()
  local label = " WARP "
  local x = math.floor((W - #label) / 2) + 1
  return x, x + #label - 1, H - 1
end

local function render(state)
  term.setBackgroundColor(colors.black)
  term.clear()
  rowMap = {}

  centered(1, "WARP DRIVE", colors.cyan)

  term.setCursorPos(1, 3)
  term.setTextColor(colors.lightGray)
  term.write(("%d / %d FE"):format(state.energy, state.capacity))
  drawBar(4, state.energy, state.capacity, W)

  if not state.hasCrystal then
    centered(LIST_TOP + 1, "No crystal inserted", colors.red)
    return
  end

  local selectedExists = false

  if #state.points == 0 then
    centered(LIST_TOP + 1, "No warp points saved", colors.gray)
  else
    local y = LIST_TOP
    for i, point in ipairs(state.points) do
      if point.selected then selectedExists = true end

      if y > LIST_BOTTOM then
        term.setCursorPos(1, y)
        term.setTextColor(colors.gray)
        term.write(("...and %d more"):format(#state.points - i + 1))
        break
      end

      term.setCursorPos(1, y)
      if point.selected then
        term.setBackgroundColor(colors.blue)
        term.setTextColor(colors.white)
      else
        term.setTextColor(colors.lightGray)
      end

      -- Pad to full width so the selected row highlights as a solid bar.
      local label = ("%d. %s (%d,%d,%d)"):format(i, point.name, point.x, point.y, point.z)
      if #label > W then label = label:sub(1, W - 2) .. ".." end
      term.write(label .. string.rep(" ", W - #label))

      term.setBackgroundColor(colors.black)
      rowMap[y] = i
      y = y + 1
    end
  end

  local canWarp = not state.charging and state.energy >= state.cost and selectedExists

  if state.charging then
    centered(H - 3, ("Charging... %.1fs"):format(state.chargeSeconds), colors.orange)
  elseif state.energy < state.cost then
    centered(H - 3, "Insufficient power", colors.red)
  elseif not selectedExists then
    centered(H - 3, "Select a destination", colors.gray)
  else
    centered(H - 3, "Ready", colors.lime)
  end

  local bx, _, by = warpButtonBounds()
  drawButton(bx, by, "WARP", canWarp)
end

-- Draws only if something visible changed. force=true is for cases where the screen was
-- overwritten by something outside the normal state (the transient failure message below).
local function refresh(force)
  local state = readState()
  if force or not sameState(state, lastState) then
    render(state)
    lastState = state
  end
end

local function handleTouch(x, y)
  local index = rowMap[y]
  if index then
    drive.selectPoint(index)
    return
  end

  local bx1, bx2, by = warpButtonBounds()
  if y == by and x >= bx1 and x <= bx2 then
    local ok, reason = drive.warp()
    if not ok then
      centered(H - 3, "Failed: " .. reason, colors.red)
      os.sleep(1.5)
      -- That message painted over the status line, so the next draw has to happen even though
      -- the drive's own state didn't change.
      refresh(true)
    end
  end
end

refresh(true)

-- Poll on a timer as well as on touch, so charge countdowns and energy levels stay live even when
-- nobody's interacting with it. Polling is cheap; the redraw is what's gated.
local timer = os.startTimer(0.5)
while true do
  local event, p1, p2, p3 = os.pullEvent()

  if event == "monitor_touch" then
    handleTouch(p2, p3)
    refresh()
  elseif event == "timer" and p1 == timer then
    refresh()
    timer = os.startTimer(0.5)
  end
end