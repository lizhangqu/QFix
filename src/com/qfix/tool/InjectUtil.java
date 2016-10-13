package com.qfix.tool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Application;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class InjectUtil {

	public static final String APP_ROOT = "/data/data/com.qfix.maindex/files";

	public static boolean inject(Application context, String dexFileName, String fooClass, boolean isAppend) {
		File file = null;
		InputStream is = null;
		FileOutputStream os = null;
		try {
			File dir = context.getFilesDir();
			String dstPath = dir != null ? dir.getAbsolutePath() : APP_ROOT;
			File tempf = new File(dstPath);
			if (!tempf.exists()) {
				tempf.mkdirs();
			}
			file = new File(dstPath, dexFileName);
			if (!file.exists()) {
				file.createNewFile();
				is = context.getAssets().open(dexFileName);
				byte[] buffer = new byte[is.available()];
				is.read(buffer);
				os = new FileOutputStream(file);
				os.write(buffer);
			}
		} catch (Throwable e) {
			return false;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {
				}
			}
			if (os != null) {
				try {
					os.close();
				} catch (Exception e) {
				}
			}
		}
		Log.d("QFixDemo", "InjectUtil inject copy dex from assets successful.");

		try {
			Class.forName("dalvik.system.LexClassLoader");
			return injectInAliyunOs(context, file.getAbsolutePath(), fooClass, isAppend);
		} catch (ClassNotFoundException e) {
		}
		boolean hasBaseDexClassLoader = true;
		try {
			Class.forName("dalvik.system.BaseDexClassLoader");
		} catch (ClassNotFoundException e) {
			hasBaseDexClassLoader = false;
		}
		if (!hasBaseDexClassLoader) {
			return injectBelowApiLevel14(context, file.getAbsolutePath(), fooClass, isAppend);
		} else {
			return injectAboveEqualApiLevel14(context, file.getAbsolutePath(), fooClass, isAppend);
		}
	}

	private static boolean injectInAliyunOs(Application app, String libPath, String fooClass, boolean isAppend) {
		PathClassLoader localClassLoader = (PathClassLoader) app.getClassLoader();
		new DexClassLoader(libPath, app.getDir("dex", 0).getAbsolutePath(), libPath, localClassLoader);
		String lexFileName = new File(libPath).getName();
		lexFileName = lexFileName.replaceAll("\\.[a-zA-Z0-9]+", ".lex");
		try {
			Class<?> classLexClassLoader = Class.forName("dalvik.system.LexClassLoader");
			Constructor<?> constructorLexClassLoader = classLexClassLoader
					.getConstructor(String.class, String.class, String.class, ClassLoader.class);
			Object localLexClassLoader = constructorLexClassLoader.newInstance(
					app.getDir("dex", 0).getAbsolutePath() + File.separator
							+ lexFileName, app.getDir("dex", 0).getAbsolutePath(), libPath, localClassLoader);
			Method methodLoadClass = classLexClassLoader.getMethod("loadClass",
					String.class);
			if (!TextUtils.isEmpty(fooClass)) {
				methodLoadClass.invoke(localLexClassLoader, fooClass);
			}
			setField(
					localClassLoader,
					PathClassLoader.class,
					"mPaths",
					appendArray(
							getField(localClassLoader, PathClassLoader.class,
									"mPaths"),
							getField(localLexClassLoader, classLexClassLoader,
									"mRawDexPath"), isAppend));
			setField(
					localClassLoader,
					PathClassLoader.class,
					"mFiles",
					combineArray(
							getField(localClassLoader, PathClassLoader.class,
									"mFiles"),
							getField(localLexClassLoader, classLexClassLoader,
									"mFiles"), isAppend));
			setField(
					localClassLoader,
					PathClassLoader.class,
					"mZips",
					combineArray(
							getField(localClassLoader, PathClassLoader.class,
									"mZips"),
							getField(localLexClassLoader, classLexClassLoader,
									"mZips"), isAppend));
			setField(
					localClassLoader,
					PathClassLoader.class,
					"mLexs",
					combineArray(
							getField(localClassLoader, PathClassLoader.class,
									"mLexs"),
							getField(localLexClassLoader, classLexClassLoader,
									"mDexs"), isAppend));
			return true;
		} catch (Throwable t) {
			t.printStackTrace();
			return false;
		}

	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private static boolean injectBelowApiLevel14(Application app, String libPath, String fooClass, boolean isAppend) {
		PathClassLoader pathClassLoader = (PathClassLoader) app.getClassLoader();
		DexClassLoader dexClassLoader = new DexClassLoader(libPath, app.getDir("dex", 0).getAbsolutePath(), libPath, app.getClassLoader());

		try {
			/*if (Build.VERSION.SDK_INT <= 8) {
				Method ensureInitMethod = dexClassLoader.getClass().getDeclaredMethod("ensureInit");
				ensureInitMethod.setAccessible(true);
				ensureInitMethod.invoke(dexClassLoader);
			}*/
			if (!TextUtils.isEmpty(fooClass)) {
				dexClassLoader.loadClass(fooClass);
			}
			setField(
					pathClassLoader,
					PathClassLoader.class,
					"mPaths",
					appendArray(
							getField(pathClassLoader, PathClassLoader.class,
									"mPaths"),
							getField(dexClassLoader, DexClassLoader.class,
									"mRawDexPath"), isAppend));
			setField(
					pathClassLoader,
					PathClassLoader.class,
					"mFiles",
					combineArray(
							getField(pathClassLoader, PathClassLoader.class,
									"mFiles"),
							getField(dexClassLoader, DexClassLoader.class,
									"mFiles"), isAppend));
			setField(
					pathClassLoader,
					PathClassLoader.class,
					"mZips",
					combineArray(
							getField(pathClassLoader, PathClassLoader.class,
									"mZips"),
							getField(dexClassLoader, DexClassLoader.class,
									"mZips"), isAppend));
			setField(
					pathClassLoader,
					PathClassLoader.class,
					"mDexs",
					combineArray(
							getField(pathClassLoader, PathClassLoader.class,
									"mDexs"),
							getField(dexClassLoader, DexClassLoader.class,
									"mDexs"), isAppend));
			return true;
		} catch (Throwable e) {
			Log.d("QFixDemo", "InjectUtil injectBelowApiLevel14 Throwable=" + e);
			e.printStackTrace();
			return false;
		}
	}

	@SuppressLint("NewApi")
	private static boolean injectAboveEqualApiLevel14(Application app,
													  String libPath, String fooClass, boolean isAppend) {
		PathClassLoader pathClassLoader = (PathClassLoader) app.getClassLoader();
		DexClassLoader dexClassLoader = new DexClassLoader(libPath, app.getDir("dex", 0).getAbsolutePath(), libPath, app.getClassLoader());
		try {
			if (!TextUtils.isEmpty(fooClass)) {
				dexClassLoader.loadClass(fooClass);
			}
			Object dexElements = combineArray(
					getDexElements(getPathList(pathClassLoader)),
					getDexElements(getPathList(dexClassLoader)), isAppend);
			Object pathList = getPathList(pathClassLoader);
			setField(pathList, pathList.getClass(), "dexElements", dexElements);
			return true;
		} catch (Throwable e) {
			Log.d("QFixDemo", "InjectUtil injectAboveEqualApiLevel14 Throwable=" + e);
			return false;
		}
	}

	private static Object getPathList(Object baseDexClassLoader)
			throws IllegalArgumentException, NoSuchFieldException,
			IllegalAccessException, ClassNotFoundException {
		return getField(baseDexClassLoader, Class.forName("dalvik.system.BaseDexClassLoader"), "pathList");
	}

	private static Object getDexElements(Object paramObject)
			throws IllegalArgumentException, NoSuchFieldException,
			IllegalAccessException {
		return getField(paramObject, paramObject.getClass(), "dexElements");
	}

	private static Object getField(Object obj, Class<?> cl, String field)
			throws NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException {
		Field localField = cl.getDeclaredField(field);
		localField.setAccessible(true);
		return localField.get(obj);
	}

	private static void setField(Object obj, Class<?> cl, String field,
								 Object value) throws NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {
		Field localField = cl.getDeclaredField(field);
		localField.setAccessible(true);
		localField.set(obj, value);
	}

	private static Object combineArray(Object arrayLhs, Object arrayRhs, boolean isAppend) {
		Class<?> localClass = arrayLhs.getClass().getComponentType();
		int lenLhs = Array.getLength(arrayLhs);
		int lenRhs = Array.getLength(arrayRhs);
		int i = isAppend ? lenLhs : lenRhs;
		int j = i + (isAppend ? lenRhs : lenLhs);
		Object result = Array.newInstance(localClass, j);
		for (int k = 0; k < j; ++k) {
			if (k < i) {
				Array.set(result, k, Array.get((isAppend ? arrayLhs : arrayRhs), k));
			} else {
				Array.set(result, k, Array.get((isAppend ? arrayRhs : arrayLhs), k - i));
			}
		}
		return result;
	}

	private static Object appendArray(Object array, Object value, boolean isAppend) {
		Class<?> localClass = array.getClass().getComponentType();
		int lenArray = Array.getLength(array);
		int i = isAppend ? lenArray : 1;
		int j = i + (isAppend ? 1 : lenArray);
		Object localObject = Array.newInstance(localClass, j);
		for (int k = 0; k < j; ++k) {
			if (k < i) {
				Array.set(localObject, k, (isAppend ? Array.get(array, k) : value));
			} else {
				Array.set(localObject, k, (isAppend ? value : Array.get(array, k - i)));
			}
		}
		return localObject;
	}

	public static boolean unloadDexElement(Application app, int index) {
		try {
			Class.forName("dalvik.system.LexClassLoader");
			return unloadDexInAliyunOs(app, index);
		} catch (ClassNotFoundException e) {
		}
		boolean hasBaseDexClassLoader = true;
		try {
			Class.forName("dalvik.system.BaseDexClassLoader");
		} catch (ClassNotFoundException e) {
			hasBaseDexClassLoader = false;
		}
		if (!hasBaseDexClassLoader) {
			return unloadDexBelowApiLevel14(app, index);
		} else {
			return unloadDexAboveEqualApiLevel14(app, index);
		}
	}

	private static boolean unloadDexInAliyunOs(Application app, int index) {
		PathClassLoader localClassLoader = (PathClassLoader) app.getClassLoader();
		try {
			setField(
					localClassLoader,
					PathClassLoader.class,
					"mPaths",
					removeElementFromArray(getField(localClassLoader, PathClassLoader.class, "mPaths"), index));

			setField(
					localClassLoader,
					PathClassLoader.class,
					"mFiles",
					removeElementFromArray(getField(localClassLoader, PathClassLoader.class, "mFiles"), index));
			setField(
					localClassLoader,
					PathClassLoader.class,
					"mZips",
					removeElementFromArray(getField(localClassLoader, PathClassLoader.class, "mZips"), index));
			setField(
					localClassLoader,
					PathClassLoader.class,
					"mLexs",
					removeElementFromArray(getField(localClassLoader, PathClassLoader.class, "mLexs"), index));
			return true;
		} catch (Throwable t) {
			t.printStackTrace();
			return false;
		}

	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private static boolean unloadDexBelowApiLevel14(Application app, int index) {
		PathClassLoader pathClassLoader = (PathClassLoader) app.getClassLoader();
		try {
			setField(
					pathClassLoader,
					PathClassLoader.class,
					"mPaths",
					removeElementFromArray(getField(pathClassLoader, PathClassLoader.class, "mPaths"), index));
			setField(
					pathClassLoader,
					PathClassLoader.class,
					"mFiles",
					removeElementFromArray(getField(pathClassLoader, PathClassLoader.class, "mFiles"), index));
			setField(
					pathClassLoader,
					PathClassLoader.class,
					"mZips",
					removeElementFromArray(getField(pathClassLoader, PathClassLoader.class, "mZips"), index));
			setField(
					pathClassLoader,
					PathClassLoader.class,
					"mDexs",
					removeElementFromArray(getField(pathClassLoader, PathClassLoader.class, "mDexs"), index));
			return true;
		} catch (Throwable e) {
			e.printStackTrace();
			return false;
		}
	}

	@SuppressLint("NewApi")
	private static boolean unloadDexAboveEqualApiLevel14(Application app, int index) {
		PathClassLoader pathClassLoader = (PathClassLoader) app.getClassLoader();
		try {
			Object dexElements = removeElementFromArray(getDexElements(getPathList(pathClassLoader)), index);
			Object pathList = getPathList(pathClassLoader);
			setField(pathList, pathList.getClass(), "dexElements", dexElements);
			return true;
		} catch (Throwable e) {
			e = null;
			return false;
		}
	}

	private static Object removeElementFromArray(Object array, int index) {
		Class<?> localClass = array.getClass().getComponentType();
		int len = Array.getLength(array);
		if (index < 0 || index >= len) {
			return array;
		}
		Object result = Array.newInstance(localClass, len - 1);
		int i = 0;
		for (int k = 0; k < len; ++k) {
			if (k != index) {
				Array.set(result, i++, Array.get(array, k));
			}
		}
		return result;
	}
}
