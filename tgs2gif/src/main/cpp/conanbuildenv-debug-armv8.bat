@echo off
chcp 65001 > nul
setlocal
echo @echo off > "%~dp0/deactivate_conanbuildenv-debug-armv8.bat"
echo echo Restoring environment >> "%~dp0/deactivate_conanbuildenv-debug-armv8.bat"
for %%v in () do (
    set foundenvvar=
    for /f "delims== tokens=1,2" %%a in ('set') do (
        if /I "%%a" == "%%v" (
            echo set "%%a=%%b">> "%~dp0/deactivate_conanbuildenv-debug-armv8.bat"
            set foundenvvar=1
        )
    )
    if not defined foundenvvar (
        echo set %%v=>> "%~dp0/deactivate_conanbuildenv-debug-armv8.bat"
    )
)
endlocal

