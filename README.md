[老文档](ReadMe.md)

基于VirtualXposed的壳子，进行了如下修改：
-----------

1. **替换sandhook** 由于VirtualXposed原先的epic对外不再开源，而且支持android9，有问题。所以底层的xposed框架，替换为sandhook。
2. **xposedinstalle去除** 由于Sandhook无需安装xposedinstalled，去除了xposedinstalled的检测逻辑
3. **新增模块管理功能**
4. **新增模拟定位功能，不再需要安装第三方的模拟位置框架**
5. **开发使用微信测试，直接被封号；暂时修改了package路径以及加入xposedhider机制；有效性待验证**

开发有用的帮助
-----------
[原wiki](https://github.com/android-hacker/VirtualXposed/wiki

[原开发手册]("https://github.com/android-hacker/VirtualXposed/wiki/Utilities-For-Xposed-Module-Developer")

1.更新代码
git clone https://github.com/simpleman1984/VirtualXposed.git -b vxp --recursive

2.重启内部app
adb shell am broadcast -a io.va.exposed.CMD -e cmd reboot

3.安装更新内部app
adb shell am broadcast -a io.va.exposed.CMD -e cmd update -e pkg <package-name>
  
4.启动内部APP
adb shell am broadcast -a io.va.exposed.CMD -e cmd launch -e pkg <package-name>
  
  
