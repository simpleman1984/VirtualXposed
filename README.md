[老文档](ReadMe.md "中文")

基于VirtualXposed的壳子，进行了如下修改：
-----------

1. **由于VirtualXposed原先的epic对外不再开源，而且支持android9，有问题。所以底层的xposed框架，替换为sandhook。
2. **由于Sandhook无需安装xposedinstalled，去除了xposedinstalled的检测逻辑
3. **新增模块管理功能
4. **新增模拟定位功能，不再需要安装第三方的模拟位置框架
5. **开发使用微信测试，直接被封号；暂时修改了package路径以及加入xposedhider机制；有效性待验证

