#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>

#define  LOG_TAG    "HotPatchTool"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)

const jint CODE_RESOLVE_PATCH_ALL_SUCCESS = 0;
const jint CODE_JAVA_PARAMETER_ERROR = 1;
const jint CODE_NATIVE_INIT_PARAMETER_ERROR = 2;
const jint CODE_LOAD_DALVIK_SO_ERROR = 3;
const jint CODE_FIND_LOADED_CLASS_ERROR = 4;
const jint CODE_REFERRER_CLASS_OBJECT_ERROR = 5;
const jint CODE_RESOLVE_CLASS_ERROR = 6;
const jint CODE_NATIVE_ITEM_PARAMETER_ERROR = 7;
const jint CODE_PATCH_CLASS_OBJECT_ERROR = 8;

#define NUM_FACTOR_PATCH 10

void* (*g_pDvmFindLoadedClass_Addr)(const char*);
void* (*g_pDvmResolveClass_Addr)(const void*, unsigned int, int);

#define ARRAY_SIZE_FIND_CLASS 3
const char* ARRAY_SYMBOL_FIND_LOADED_CLASS[ARRAY_SIZE_FIND_CLASS] = {
	"_Z18dvmFindLoadedClassPKc",
	"_Z18kvmFindLoadedClassPKc",
	"dvmFindLoadedClass"
};

#define ARRAY_SIZE_RESOLVE_CLASS 2
const char* ARRAY_SYMBOL_RESOLVE_CLASS[ARRAY_SIZE_RESOLVE_CLASS] = {
	"dvmResolveClass",
	"vResolveClass"
};

jint Java_com_qfix_tool_HotPatchTool_nativeResolvePatchClass(JNIEnv* env,
		jobject thiz, jobjectArray referrerClassList, jlongArray classIdxList, jint size) {
	LOGI("enter nativeResolvePatchClass");
	int referrerClassSize = (*env)->GetArrayLength(env, referrerClassList);
	int classIdxSize = (*env)->GetArrayLength(env, classIdxList);
	if (size <= 0 || referrerClassSize != size || classIdxSize != size) {
		LOGE("CODE_NATIVE_INIT_PARAMETER_ERROR");
		return CODE_NATIVE_INIT_PARAMETER_ERROR;
	}
	jlong* jClassIdxArray = (*env)->GetLongArrayElements(env, classIdxList, 0);
	if (jClassIdxArray == 0) {
		LOGE("CODE_NATIVE_INIT_PARAMETER_ERROR");
		return CODE_NATIVE_INIT_PARAMETER_ERROR;
	}

	void* handle = 0;
	handle = dlopen("/system/lib/libdvm.so", RTLD_LAZY);
	if (handle) {
		void* findFunc = 0;
		int i = 0;
		while(i < ARRAY_SIZE_FIND_CLASS) {
			findFunc = dlsym(handle, ARRAY_SYMBOL_FIND_LOADED_CLASS[i]);
			if (findFunc) {
				break;
			}
			i++;
		}
		if (findFunc) {
			g_pDvmFindLoadedClass_Addr = findFunc;
			void* resolveFunc = 0;
			i = 0;
			while(i < ARRAY_SIZE_RESOLVE_CLASS) {
				resolveFunc = dlsym(handle, ARRAY_SYMBOL_RESOLVE_CLASS[i]);
				if (resolveFunc) {
					break;
				}
				i++;
			}
			if (resolveFunc) {
				g_pDvmResolveClass_Addr = resolveFunc;
				i = 0;
				while(i < size) {
					jstring jClassItem = (jstring)((*env)->GetObjectArrayElement(env, referrerClassList, i));
					const char* classItem = (*env)->GetStringUTFChars(env, jClassItem, 0);
					if (classItem == 0) {
						(*env)->ReleaseLongArrayElements(env, classIdxList, jClassIdxArray, 0);
						LOGE("CODE_NATIVE_ITEM_PARAMETER_ERROR=%d", i);
						return NUM_FACTOR_PATCH * i + CODE_NATIVE_ITEM_PARAMETER_ERROR;
					}
					if (strlen(classItem) < 5 || jClassIdxArray[i] < 0) {
						(*env)->ReleaseLongArrayElements(env, classIdxList, jClassIdxArray, 0);
						(*env)->ReleaseStringUTFChars(env, jClassItem, classItem);
						LOGE("CODE_NATIVE_ITEM_PARAMETER_ERROR=%d", i);
						return NUM_FACTOR_PATCH * i + CODE_NATIVE_ITEM_PARAMETER_ERROR;
					}
					void* referrerClassObj = g_pDvmFindLoadedClass_Addr(classItem);
					if (referrerClassObj) {
						void* resClassObj = g_pDvmResolveClass_Addr(referrerClassObj, (unsigned int)jClassIdxArray[i], 1);
						if (!resClassObj) {
							(*env)->ReleaseLongArrayElements(env, classIdxList, jClassIdxArray, 0);
							(*env)->ReleaseStringUTFChars(env, jClassItem, classItem);
							LOGE("CODE_PATCH_CLASS_OBJECT_ERROR=%d", i);
							return NUM_FACTOR_PATCH * i + CODE_PATCH_CLASS_OBJECT_ERROR;
						}
					} else {
						(*env)->ReleaseLongArrayElements(env, classIdxList, jClassIdxArray, 0);
						(*env)->ReleaseStringUTFChars(env, jClassItem, classItem);
						LOGE("CODE_REFERRER_CLASS_OBJECT_ERROR=%d", i);
						return NUM_FACTOR_PATCH * i + CODE_REFERRER_CLASS_OBJECT_ERROR;
					}
					(*env)->ReleaseStringUTFChars(env, jClassItem, classItem);
					i++;
				}
			} else {
				(*env)->ReleaseLongArrayElements(env, classIdxList, jClassIdxArray, 0);
				LOGE("CODE_RESOLVE_CLASS_ERROR");
				return CODE_RESOLVE_CLASS_ERROR;
			}
		} else {
			(*env)->ReleaseLongArrayElements(env, classIdxList, jClassIdxArray, 0);
			LOGE("CODE_FIND_LOADED_CLASS_ERROR");
			return CODE_FIND_LOADED_CLASS_ERROR;
		}
	} else {
		(*env)->ReleaseLongArrayElements(env, classIdxList, jClassIdxArray, 0);
		LOGE("CODE_LOAD_DALVIK_SO_ERROR");
		return CODE_LOAD_DALVIK_SO_ERROR;
	}
	(*env)->ReleaseLongArrayElements(env, classIdxList, jClassIdxArray, 0);
	LOGI("CODE_RESOLVE_PATCH_ALL_SUCCESS");
	return CODE_RESOLVE_PATCH_ALL_SUCCESS;
}
