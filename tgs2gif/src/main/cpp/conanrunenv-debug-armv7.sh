echo "echo Restoring environment" > "D:\Project\parabox-extension-telegram\tgs2gif\src\main\cpp\deactivate_conanrunenv-debug-armv7.sh"
for v in 
do
    is_defined="true"
    value=$(printenv $v) || is_defined="" || true
    if [ -n "$value" ] || [ -n "$is_defined" ]
    then
        echo export "$v='$value'" >> "D:\Project\parabox-extension-telegram\tgs2gif\src\main\cpp\deactivate_conanrunenv-debug-armv7.sh"
    else
        echo unset $v >> "D:\Project\parabox-extension-telegram\tgs2gif\src\main\cpp\deactivate_conanrunenv-debug-armv7.sh"
    fi
done

