package com.xuyongjun.imageloader.utils;

/**
 * ============================================================
 * 作 者 : XYJ
 * 版 本 ： 1.0
 * 创建日期 ： 2016/8/5 15:12
 * 描 述 ：图片加载类
 * 修订历史 ：
 * ============================================================
 **/

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * 1.编写单例
 */
public class ImageLoader {

    private static ImageLoader mInstance;

    /**
     * 图片缓存核心
     */
    private LruCache<String, Bitmap> mLruCache;

    /**
     * 线程池对象
     */
    private ExecutorService mThreadPool;

    /**
     * 默认线程池数量
     */
    private static final int DEAFULT_THREAD_COUNT = 1;

    /**
     * 图片加载策略
     * FIFO:First In Last Out 先进先出
     * LIFO:Last In First Out 后进先出
     */
    public enum Type {
        FIFO, LIFO
    }

    /**
     * 队列调度方式
     */
    private Type mType = Type.LIFO;

    /**
     * 任务队列
     */
    private LinkedList<Runnable> mTaskQueue;

    /**
     * 后台轮询线程
     */
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;

    /**
     * UI线程中的Handler
     */
    private Handler mUIHandler;

    /**
     * 信号量，用于线程同步，java api
     * <p/>
     * mSemaphorePoolThreadHandler:用于mPoolThreadHandler初始化的时候线程并发安全
     * mSemaphoreThreadPool：用于加载策略
     */
    private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);
    private Semaphore mSemaphoreThreadPool;

    private ImageLoader(int threadCount, Type type)
    {
        init(threadCount, type);
    }

    /**
     * 初始化操作
     *
     * @param threadCount
     * @param type
     */
    private void init(int threadCount, Type type)
    {

        /*
        这里存在线程安全，需要用信号量来维护线程同步
         */
        mPoolThread = new Thread() {
            @Override
            public void run()
            {
                /*
                后台不断轮询
                 */
                Looper.prepare();

                mPoolThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg)
                    {
                        //线程池取出一个任务进行执行
                        mThreadPool.execute(getTask());
                        try {
                            mSemaphoreThreadPool.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };

                //释放一个信号量
                mSemaphorePoolThreadHandler.release();

                Looper.loop();
            }
        };
        mPoolThread.start();

        //获取我们应用最大的可用内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemoru = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheMemoru) {
            @Override
            protected int sizeOf(String key, Bitmap value)
            {
                return value.getRowBytes() * value.getHeight();//图片一行的字节*高=图片的大小
            }
        };

        //创建线程池
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList<Runnable>();
        mType = type;

        mSemaphoreThreadPool = new Semaphore(threadCount);
    }

    /**
     * 从任务队列取出一个方法
     *
     * @return
     */
    private Runnable getTask()
    {
        if (mType == Type.FIFO) {
            return mTaskQueue.removeFirst();
        } else if (mType == Type.LIFO) {
            return mTaskQueue.removeLast();
        }
        return mTaskQueue.removeLast();
    }

    public static ImageLoader getInstance()
    {
        if (mInstance == null) {
            synchronized (ImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new ImageLoader(DEAFULT_THREAD_COUNT, Type.LIFO);
                }
            }
        }
        return mInstance;
    }

    /**
     * 获取对象实例
     * @param threadCount 线程数量
     * @param type 加载策略
     * @return
     */
    public static ImageLoader getInstance(int threadCount,Type type)
    {
        if (mInstance == null) {
            synchronized (ImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new ImageLoader(threadCount, type);
                }
            }
        }
        return mInstance;
    }

    /**
     * 根据Path对ImageView设置图片
     *
     * @param path
     * @param imageView
     */
    public void loadImage(final String path, final ImageView imageView)
    {
        //防止图片错乱,GridView复用问题
        imageView.setTag(path);

        if (mUIHandler == null) {
            mUIHandler = new Handler() {
                @Override
                public void handleMessage(Message msg)
                {
                    ImageHolder holder = (ImageHolder) msg.obj;
                    String path = holder.path;
                    Bitmap bm = holder.bm;
                    ImageView imageview = holder.imageView;

                    //将Path与imageView之前设置的Tag进行比较，防止图片出现混乱
                    if (imageview.getTag().equals(path)) {
                        imageview.setImageBitmap(bm);
                    }
                }
            };
        }

        //根据Path在缓存中获取Bitmap
        Bitmap bm = getBitmapFromLruCache(path);

        /*
        如果缓存中有数据的话直接通知UIHandler设置ImageView
        没有的话交给Task
         */
        if (bm != null) {
            refreashBitmap(path, imageView, bm);
        } else {
            addTasks(new Runnable() {
                @Override
                public void run()
                {
                    //1.获取图片，压缩图片获取图片大小
                    ImageSize imageSize = getImageViewSize(imageView);
                    //2.压缩图片
                    Bitmap bm = decodeSampleBitmapFromPath(path, imageSize.width, imageSize.height);
                    //3.把图片加入到缓存中
                    addBitmapToLruCache(path, bm);

                    refreashBitmap(path, imageView, bm);

                    mSemaphoreThreadPool.release();
                }
            });
        }
    }

    private void refreashBitmap(String path, ImageView imageView, Bitmap bm)
    {
        Message message = Message.obtain();

        ImageHolder holder = new ImageHolder();
        holder.bm = bm;
        holder.imageView = imageView;
        holder.path = path;
        message.obj = holder;

        mUIHandler.sendMessage(message);
    }

    /**
     * 添加图片到LruCache中
     *
     * @param path
     * @param bm
     */
    private void addBitmapToLruCache(String path, Bitmap bm)
    {
        //当缓存中不存在的时候
        if (getBitmapFromLruCache(path) == null) {
            //当传入的Bitmap不为null的时候
            if (bm != null) {
                mLruCache.put(path, bm);
            }
        }
    }

    /**
     * 压缩图片
     *
     * @param path
     * @param width
     * @param height
     * @return
     */
    private Bitmap decodeSampleBitmapFromPath(String path, int width, int height)
    {
        //获取图片宽高，并不把图片加载到内存中
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = caculateInSampleSize(options, width, height);

        //将图片加载到内存中
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;
    }

    /**
     * 根据需求的宽和高以及图片实际的宽和高计算SimpleSize
     *
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private int caculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
    {
        //源图片的宽和高
        int width = options.outWidth;
        int height = options.outHeight;

        int inSampleSize = 1;//默认实际大小

        if (width > reqWidth || height > reqHeight) {
            //计算实际宽高和需求宽高的比率
            int widthRadio = Math.round(width * 1.0f / reqWidth);
            int heightRadio = Math.round(height * 1.0f / reqHeight);

            inSampleSize = Math.max(widthRadio, heightRadio);
        }
        return inSampleSize;
    }

    /**
     * 获取ImageView大小
     *
     * @param imageView
     */

    private ImageSize getImageViewSize(ImageView imageView)
    {
        ImageSize imageSize = new ImageSize();

        //拿到屏幕的参数
        DisplayMetrics metrics = imageView.getContext().getResources().getDisplayMetrics();

        //拿到ImageView在布局中的参数
        ViewGroup.LayoutParams lp = imageView.getLayoutParams();

        //判断ImageView的尺寸，如果设置的Wrap_content的话需要进行判断
        int width = imageView.getWidth();
        if (width <= 0) {
            width = lp.width;//获取ImageView在layout中的声明
        }
        if (width <= 0) {
            width = getImageViewFieldValue(imageView,"mMaxWidth");//检查最大值
        }
        if (width <= 0) {
            width = metrics.widthPixels;
        }

        int height = imageView.getHeight();
        if (height <= 0) {
            height = lp.height;
        }
        if (width <= 0) {
            height = getImageViewFieldValue(imageView,"mMaxHeight");
        }
        if (width <= 0) {
            height = metrics.heightPixels;
        }

        imageSize.height = height;
        imageSize.width = width;

        return imageSize;
    }

    /**
     * 通过反射获取ImageView的某个属性，这个是获取ImageView的MaxWidth和MaxHeight
     *
     * @param obj
     * @param fieldName
     * @return
     */
    private static int getImageViewFieldValue(Object obj, String fieldName)
    {
        int value = 0;
        try {
            Field field = ImageView.class.getField(fieldName);
            field.setAccessible(true);//暴力访问
            int fieldValue = field.getInt(obj);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
                value = fieldValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    private synchronized void addTasks(Runnable runnable)
    {
        mTaskQueue.add(runnable);

        //获取一个许可
        try {
            if (mPoolThreadHandler == null)
                //获取一个信号量
                mSemaphorePoolThreadHandler.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mPoolThreadHandler.sendEmptyMessage(0x0001);
    }

    /**
     * 根据Path在缓存中获取Bitmap
     *
     * @param key
     * @return
     */
    private Bitmap getBitmapFromLruCache(String key)
    {
        return mLruCache.get(key);
    }

    /**
     * 封装LruCache中图片的数据
     */
    private class ImageHolder {
        Bitmap bm;
        String path;
        ImageView imageView;
    }

    /**
     * 存放图片宽高的类
     */
    private class ImageSize {
        int width;
        int height;
    }
}
