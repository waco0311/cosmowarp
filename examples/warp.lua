-- Example CC: Tweaked script for cosmowarp's Warp Drive peripheral.
-- Place a computer adjacent to a Warp Drive (or connect one with a wired modem), then run this.
--
-- This is a starting point, not a finished UI -- the peripheral deliberately exposes data and
-- controls only, so monitor layouts and automation logic are yours to write.

local drive = peripheral.find("warp_drive")
if not drive then
  error("No Warp Drive found. Place the computer next to one, or attach it with a wired modem.")
end

local function printStatus()
  local energy, capacity = drive.getEnergy(), drive.getEnergyCapacity()
  local cost = drive.getWarpCost()

  print(("Energy: %d / %d FE"):format(energy, capacity))
  print(("Warp cost: %d FE  (%s)"):format(
    cost, energy >= cost and "ready" or "insufficient"))

  if not drive.hasCrystal() then
    print("No crystal or Memory Card inserted.")
    return
  end

  local points = drive.getWarpPoints()
  if #points == 0 then
    print("No warp points saved.")
    return
  end

  print("Saved points:")
  for i, point in ipairs(points) do
    print(("  %d. %s  (%d, %d, %d)  [%s]%s"):format(
      i, point.name, point.x, point.y, point.z, point.dimension,
      point.selected and "  <- selected" or ""))
  end
end

-- Warps to a point given by index, name, or id, and waits for the jump to finish.
local function warpTo(target)
  if not drive.selectPoint(target) then
    print("No such warp point: " .. tostring(target))
    return false
  end

  local ok, reason = drive.warp()
  if not ok then
    print("Warp failed: " .. reason)
    return false
  end

  while drive.isCharging() do
    print(("Charging... %.1fs"):format(drive.getChargeSeconds()))
    os.sleep(0.5)
  end

  print("Jump complete.")
  return true
end

printStatus()

-- Uncomment to warp to the first saved point:
-- warpTo(1)

-- Or by name:
-- warpTo("Home Base")
