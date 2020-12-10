if #arg < 1 then
    local programName = arg[0] or fs.getName(shell.getRunningProgram())
    print("Usage: " .. programName .. " <program> <arguments>")
    return
end

if not settings.get("bios.strict_globals", false) then
    print("Strict globals are disabled, run the program directly instead.")
end

_ENV._allow_globals = true

local returns = table.pack((shell.execute or shell.run)(table.unpack(arg, 1, #arg)))

_ENV._allow_globals = false

return table.unpack(returns, 1, returns.n)
