This mod is for using [HOCON](https://github.com/lightbend/config) for resource packs or data packs  
HOCON's syntax is more flexible and supports advanced syntax such as variables and include, which can reduce boilerplate code.  
The mod will load any file in packs ends with `.hocon` with HOCON parser and trick the game to think it's a json file.  
So that it shouldn't conflict with any packs that already use the json format.  

Need to enable in pack.mcmeta lie below:
```json
{
    "pack": {
        "pack_format": 15,
        "description": {
            "translate": "settingsun.pack",
            "fallback": "Assets of SettingSun"
        },
        "supported_formats": [15, 22]
    },
    "hoconresourceloader" : {
        "enabled": true
    }
}
```

## Advantage of HOCON
- `include` url, file (Can be used for resource/data pack config), minecraft resource by identifier
- With `include`, you can include the origin json file and modify the value instead of edit the whole file. Far more convenient than the JsonPatch
- See more on https://github.com/lightbend/config/blob/main/HOCON.md

## Examples
The result of the resource pack at https://github.com/SettingDust/HoconResourceLoader/tree/main/mod/run/resourcepacks/test-resources  
- https://github.com/SettingDust/HoconResourceLoader/blob/main/mod/run/resourcepacks/test-resources/assets/minecraft/models/item/stone.hocon  
    ```hocon
    # The current directory to read file relative is minecraft run dir.
    # `.minecraft` or `.minecraft/versions/{NAME}` dir in commonly
    # At here is `{PROJECT_DIR}/mod/run`
    include file("dirt.hocon")
    ```
    ![img.png](https://raw.githubusercontent.com/SettingDust/HoconResourceLoader/main/docs/img.png)  
- https://github.com/SettingDust/HoconResourceLoader/blob/main/mod/run/resourcepacks/test-resources/assets/minecraft/models/item/dirt.hocon  
    ```hocon
    parent: "minecraft:block/stone"
    ```
    ![img_1.png](https://raw.githubusercontent.com/SettingDust/HoconResourceLoader/main/docs/img_1.png)  
- https://github.com/SettingDust/HoconResourceLoader/blob/main/mod/run/resourcepacks/test-resources/assets/minecraft/models/item/oak_log.hocon  
    ```hocon
    # Support identifier
    include "minecraft:models/item/dirt.json"
    ```
    ![img_2.png](https://raw.githubusercontent.com/SettingDust/HoconResourceLoader/main/docs/img_2.png)  
- https://github.com/SettingDust/HoconResourceLoader/blob/main/mod/run/resourcepacks/test-resources/assets/minecraft/models/item/oak_wood.hocon  
    ```hocon
    # Will try to find under the starting path that passed to resource manager.
    # It's `models/` here
    include "minecraft:item/dirt.json"
    ```
    ![img_3.png](https://raw.githubusercontent.com/SettingDust/HoconResourceLoader/main/docs/img_3.png)  
- https://github.com/SettingDust/HoconResourceLoader/blob/main/mod/run/resourcepacks/test-resources/assets/minecraft/models/item/torch.hocon  
    ```hocon
    # Specifically, include json files won't be resolved to hocon.
    # So, you can do patch for the json file and provide the hocon file as json to the game.
    include "minecraft:models/item/torch.json"
    
    textures.layer0 = "minecraft:block/soul_torch"
    ```
    ![img_4.png](https://raw.githubusercontent.com/SettingDust/HoconResourceLoader/main/docs/img_4.png)
## Credit
Icon from https://www.flaticon.com/free-icon/folder_3767084#
