This mod is for using [HOCON](https://github.com/lightbend/config) for resource packs or data packs  
HOCON's syntax is more flexible and supports advanced syntax such as variables and include, which can reduce boilerplate code.  
The mod will load any file in packs ends with `.hocon` with HOCON parser and trick the game to think it's a json file.  
So that it shouldn't conflict with any packs that already use the json format.  

## Advantage of HOCON
- `include` url, file (Can be used for resource/data pack config), minecraft resource by identifier
- With `include`, you can include the origin json file and modify the value instead of edit the whole file. Far more convenient than the JsonPatch
- See more on https://github.com/lightbend/config/blob/main/HOCON.md

## Examples
The result of the resource pack at https://github.com/SettingDust/HoconResourceLoader/tree/main/mod/run/resourcepacks/test-resources  
- https://github.com/SettingDust/HoconResourceLoader/blob/main/mod/run/resourcepacks/test-resources/assets/minecraft/models/item/stone.hocon  
![img.png](https://raw.githubusercontent.com/SettingDust/HoconResourceLoader/main/docs/img.png)  
- https://github.com/SettingDust/HoconResourceLoader/blob/main/mod/run/resourcepacks/test-resources/assets/minecraft/models/item/dirt.hocon  
![img_1.png](https://raw.githubusercontent.com/SettingDust/HoconResourceLoader/main/docs/img_1.png)  
- https://github.com/SettingDust/HoconResourceLoader/blob/main/mod/run/resourcepacks/test-resources/assets/minecraft/models/item/oak_log.hocon  
![img_2.png](https://raw.githubusercontent.com/SettingDust/HoconResourceLoader/main/docs/img_2.png)  
- https://github.com/SettingDust/HoconResourceLoader/blob/main/mod/run/resourcepacks/test-resources/assets/minecraft/models/item/oak_wood.hocon  
![img_3.png](https://raw.githubusercontent.com/SettingDust/HoconResourceLoader/main/docs/img_3.png)  
- https://github.com/SettingDust/HoconResourceLoader/blob/main/mod/run/resourcepacks/test-resources/assets/minecraft/models/item/torch.hocon  
![img_4.png](https://raw.githubusercontent.com/SettingDust/HoconResourceLoader/main/docs/img_4.png)
## Credit
Icon from https://www.flaticon.com/free-icon/folder_3767084#
