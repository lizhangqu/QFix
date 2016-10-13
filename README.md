# QFix
		
QFix是手Q团队近期推出的一种新的android热补丁方案，在不影响app运行时性能（无需插桩去preverify）的前提下有效地规避了dalvik下"unexpected DEX"的异常，而且还是很轻量级的实现：一行代码调用一个简单的方法就能办到。



该组件还是基于java classLoader的补丁方案，但对dalvik下“unexpected DEX”异常提供了全新的轻量级的解决思路。



PatchTool.jar




		提供补丁注入、卸载的java接口，封装补丁类resolve的java接口




libResolvePatch.so




		提供arm/x86平台库


		提供resolve补丁类的native接口




ResolvePatchTool.jar




		自动输出补丁类所在的dex id和classIdx




不用编译时插桩，也不用对补丁dex进行全量合成，dex安装后，只需调用以下方法：（示例）




String[] referrerList = new String[] {"LFoo2;"};




long[] classIdList = new long[] {2};




HotPatchTool.resolvePatchClass(this, referrerList, classIdList, 1);