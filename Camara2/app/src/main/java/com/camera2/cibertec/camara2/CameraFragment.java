package com.camera2.cibertec.camara2;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


public class CameraFragment extends android.app.Fragment
        implements View.OnClickListener, FragmentCompat.OnRequestPermissionsResultCallback {


    //variables que pasan del activity
    public final static String ARG_NRO_GUIA = "NRO_GUIA";
    public final static String ARG_TOMA_FOTO_CANTIDAD= "ARG_TOMA_FOTO_CANTIDAD";
    public final static String ARG_TOMA_FOTO_CANTIDAD_EXISTENTE = "ARG_TOMA_FOTO_CANTIDAD_EXISTENTE";
    public final static String ARG_MOTIVADO = "ARG_MOTIVADO";

    private String NRO_GUIA ; // NRO_GUIA Actual que toma el valor del activity
    private String TOMA_FOTO_CANTIDAD ; // Cantidad de fotos total que debe tomar.
    private String TOMA_FOTO_CANTIDAD_EXISTENTE ; // Cantidad de fotos pendientes por tomar.
    public final static String ARG_LISTA_GUIAS= "LISTA_GUIA";
    ArrayList<HashMap<String, String>> LISTA_GUIAS; //lista de guias que vienen en el activity
    private int IMG_TOMADA_VISTA_PREVIA=0 ;//variable para controlar que no capture la imagen cuando este en modo de visualizar la imagen recien tomada
    private String MOTIVADO ; // si esta o no Motivado que toma el valor del activity

    private static Activity mInstanceActivity;


    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";


    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "CameraFragment";

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            Log.i(TAG, "onSurfaceTextureAvailable: ");
            if (IMG_TOMADA_VISTA_PREVIA==0) { // si no esta en la vista previa de la i
                openCamera(width, height);
            }

        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mCameraId;

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;
    //private static ImageView mImageViewImagenPrevia;
    private static SubsamplingScaleImageView mImageViewImagenPrevia;
    //private static LinearLayout mLLayoutImageViewImagenPrevia;
    private static RelativeLayout mRlayoutContenedor;


    private static RelativeLayout mRLayoutAceptarCancelar;
    private static TextView txtvGuia;
    private static String nombreCarpetaImagenes;
    private static String nombreCarpetaImagenesDCIM;

    private static FrameLayout mFrameLayoutTomarFoto;


    private static Context mContext;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * The {@link Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.i(TAG, "CameraDevice.StateCallback onOpened: ");
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.i(TAG, "CameraDevice.StateCallback onDisconnected: ");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Log.i(TAG, "CameraDevice.StateCallback onError: ");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;

    /**
     * This is the output file for our picture.
     */
    private File mFile;
    private File mRutaFile; //donde se guar

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.i(TAG, "ImageReader.OnImageAvailableListener onImageAvailable: ");
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
        }

    };

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    private int mState = STATE_PREVIEW;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * Whether the current camera device supports Flash or not.
     */
    private boolean mFlashSupported;

    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {

            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    //Log.i(TAG, "CameraCaptureSession.CaptureCallback process: ");
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };




    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        Log.i(TAG, "showToast: ");
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {
        Log.i(TAG, "chooseOptimalSize: ");
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public static CameraFragment newInstance() {
        Log.i(TAG, "CameraFragment newInstance: ");
        return new CameraFragment();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        Log.i(TAG, "onViewCreated: ");
        //ojo que al girar no esta estos botones en landscape
        view.findViewById(R.id.camara_imgvCamaraTomarFoto).setOnClickListener(this);
        view.findViewById(R.id.camara_layoutCamaraTomarFoto).setOnClickListener(this);
        view.findViewById(R.id.camara_layoutConfirmar).setOnClickListener(this);
        view.findViewById(R.id.camara_imgvConfirmar).setOnClickListener(this);
        view.findViewById(R.id.camara_layoutEliminar).setOnClickListener(this);
        view.findViewById(R.id.camara_imgvEliminar).setOnClickListener(this);

//        view.findViewById(R.id.info).setOnClickListener(this);
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.camara_TextureViewTexture);
        mFrameLayoutTomarFoto=(FrameLayout) view.findViewById(R.id.camara_FrameLayoutTomarFoto);

        //mImageViewImagenPrevia=(ImageView)view.findViewById(R.id.camara_ImgvImagenPrevia);
        //mLLayoutImageViewImagenPrevia=(LinearLayout)view.findViewById(R.id.camara_LLayoutImagenPrevia);

        mRlayoutContenedor=(RelativeLayout) view.findViewById(R.id.camara_rlayoutContenedor);
        mImageViewImagenPrevia = (SubsamplingScaleImageView)view.findViewById(R.id.camara_ImgvImagenPrevia);

        mRLayoutAceptarCancelar=(RelativeLayout)view.findViewById(R.id.camara_rlayoutAceptarCancelar);
        mRLayoutAceptarCancelar=(RelativeLayout)view.findViewById(R.id.camara_rlayoutAceptarCancelar);

        nombreCarpetaImagenes=getResources().getString(R.string.camara_nombre_carpeta_imagenes);
        nombreCarpetaImagenesDCIM=getResources().getString(R.string.camara_nombre_carpeta_imagenes_dcim);

        mContext = getActivity().getApplicationContext();
        mInstanceActivity=getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onActivityCreated: ");
        super.onActivityCreated(savedInstanceState);
        //Log.i(TAG, "onActivityCreated: " + NRO_GUIA  );
        //mFile = new File(getActivity().getExternalFilesDir(null), NRO_GUIA  + ".jpg");
    }

    @Override
    public void onStart() {
        super.onStart();
        Bundle args = getArguments();

        txtvGuia= (TextView) getActivity().findViewById(R.id.camara_txtNro_guia);

        if (args != null) {

            NRO_GUIA = args.getString(ARG_NRO_GUIA);
            TOMA_FOTO_CANTIDAD=args.getString(ARG_TOMA_FOTO_CANTIDAD);
            Log.i(TAG, "onStart: cantidad fotos" + TOMA_FOTO_CANTIDAD);
            TOMA_FOTO_CANTIDAD_EXISTENTE=args.getString(ARG_TOMA_FOTO_CANTIDAD_EXISTENTE);
            LISTA_GUIAS= (ArrayList)args.getSerializable(ARG_LISTA_GUIAS);
            MOTIVADO=args.getString(ARG_MOTIVADO);

            setDatosTomaFoto();

        } else {
            mFile = new File(getActivity().getExternalFilesDir(null), "sin_nombre.jpg");
            txtvGuia.setText("");
            Log.i(TAG, "onStart" + "no pasaron los parametros");
        }
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume: ");
        super.onResume();
        startBackgroundThread();

        Log.i(TAG, "onResume: IMG_TOMADA_VISTA_PREVIA " + Integer.toString(IMG_TOMADA_VISTA_PREVIA));

        if (IMG_TOMADA_VISTA_PREVIA==0){ // si no esta en la vista previa de la i

            // When the screen is turned off and turned back on, the SurfaceTexture is already
            // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
            // a camera and start preview from here (otherwise, we wait until the surface is ready in
            // the SurfaceTextureListener).
            if (mTextureView.isAvailable()) {
                Log.i(TAG, "onResume: " + "mTextureView available");
                openCamera(mTextureView.getWidth(), mTextureView.getHeight());
            } else {
                Log.i(TAG, "onResume: " + "mTextureView unvailable");
                mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
            }
        }
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause: ");
        closeCamera();
        stopBackgroundThread();
        super.onPause();

    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop: ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: ");
    }

    private void requestCameraPermission() {
        Log.i(TAG, "requestCameraPermission: ");
        if (FragmentCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            FragmentCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionsResult: ");
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(getString(R.string.request_permission))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private void setUpCameraOutputs(int width, int height) {
        Log.i(TAG, "setUpCameraOutputs: ");
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // For still image captures, we use the largest available size.
                // setear el tamanio de la resolucion de la imagen
                Size[] mSize =map.getOutputSizes(ImageFormat.JPEG);
                Size largest=null;

                String cameraResoluciones="";

                for (int i=0; i<mSize.length; i++){

                    //if (mSize[i].toString().equals("1280x960")){//1280x960--1280x720 2048x1536
                    if (mSize[i].toString().equals("1280x960")){
                        largest=mSize[i];
                    }

                    Log.i(TAG, "setUpCameraOutputs: " + mSize[i].toString());
                    cameraResoluciones=cameraResoluciones + mSize[i].toString() + Character.toString((char)13);
                }
               // mostrarMensaje(cameraResoluciones);
                //si no se encontro el tamanio tomar el maximo
                if (largest==null){
                    for (int i=0; i<mSize.length; i++){

                        if (mSize[i].toString().equals("1600x1200")){//1280x960--1280x720 2048x1536
                            largest=mSize[i];
                        }
                    }
                }
                if (largest==null){
                    for (int i=0; i<mSize.length; i++){

                        if (mSize[i].toString().equals("1280x720")){//1280x960--1280x720 2048x1536
                            largest=mSize[i];
                        }
                    }
                }
                if (largest==null){
                    largest = Collections.min(
                            Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                            new CompareSizesByArea());
                }
                Log.i(TAG, "setUpCameraOutputs: " + largest.getWidth() + "xxxxxxx" +  largest.getHeight( ));
                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();

                //Log.i(TAG, "setUpCameraOutputs: antes del defaultDisplay" + Integer.toString( largest.getHeight()));

                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x; //1200
                int maxPreviewHeight =displaySize.y;//1600

                // Log.i(TAG, "setUpCameraOutputs: maxWidth" + Integer.toString(maxPreviewWidth));
                // Log.i(TAG, "setUpCameraOutputs: maxHeight" + Integer.toString(maxPreviewHeight));

                if (swappedDimensions) {
                    Log.i(TAG, "entra a la rotacion" + Integer.toString(maxPreviewWidth));

                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                //Log.i(TAG, "setUpCameraOutputs: maxWidth  2 " + Integer.toString(maxPreviewWidth));
                //Log.i(TAG, "setUpCameraOutputs: maxHeight 2 " + Integer.toString(maxPreviewHeight));

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                //Nota se reemplaza SurfaceTexture.class por ImageFormat.JPEG
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {

                    mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());

                } else {

                    mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                    //mTextureView.setAspectRatio(9, 16);

                }
                //mostrarMensaje("Tamanio pantalla: witdh: " + mPreviewSize.getWidth()  + " heigth: " + mPreviewSize.getHeight());
                //mostrarMensaje("Tamanio pantalla: witdh: " + mTextureView.mWidth  + " heigth: " + mTextureView.mHeight);
                //Log.i(TAG, "setUpCameraOutputs: " + mTextureView.mWidth  + "xxxx" + mTextureView.mHeight);

                // Check if the flash is supported.
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }

    /**
     * Opens the camera specified by {@link CameraFragment#mCameraId}.
     */
    private void openCamera(int width, int height) {
        Log.i(TAG, "openCamera: " + width + "x"  + height);
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {

        Log.i(TAG, "closecamera: ");
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        Log.i(TAG, "startBackgroundThread: ");
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {

        Log.i(TAG, "stopBackgroundThread: ");
        if (mBackgroundThread!=null) {// no debe ser null para que pueda salir

            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        Log.i(TAG, "createCameraPreviewSession: ");
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);//CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                                //                            CaptureRequest.CONTROL_AF_TRIGGER_START);
                                // Flash is automatically enabled when necessary.
                                /*Rect mCropRegion=new Rect(500, 375, 1000, 750);
                                mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION,mCropRegion);*/
                                setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();

                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Log.i(TAG, "configureTransform: ");
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * Initiate a still image capture.
     */
    private void takePicture() {
        Log.i(TAG, "takePicture: ");
        lockFocus();
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        Log.i(TAG, "lockFocus: ");
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
        Log.i(TAG, "captureStillPicture: ");
        try {
            final Activity activity = getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    showToast("Se tomó la Foto"); //"Saved "+ mFile
                   /* Log.d(TAG, "Se tomó la Foto");
                    Log.d(TAG, mFile.toString());*/
                    unlockFocus();

                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        Log.i(TAG, "getOrientation: ");
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        Log.i(TAG, "unlockFocus: ");
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        Log.i(TAG, "onClick: ");
        switch (view.getId()) {
            case R.id.camara_layoutCamaraTomarFoto: {
                takePicture();
                break;
            }
            case R.id.camara_imgvCamaraTomarFoto: {
                takePicture();
                break;
            }
            case R.id.camara_layoutConfirmar: {
                confirmarFoto();
                break;
            }
            case R.id.camara_imgvConfirmar: {
                confirmarFoto();
                break;
            }
            case R.id.camara_layoutEliminar: {
                Activity activity = getActivity();
                if (null != activity) {
                    consultarAUsuarioEliminarFotoDeGuia(activity,
                            getResources().getString(R.string.camara_msg_eliminar_title),
                            getResources().getString(R.string.camara_msg_eliminar_contenido),
                            getResources().getString(R.string.btn_aceptar_texto),
                            getResources().getString(R.string.btn_cancelar_texto));


                }
                break;
            }
            case R.id.camara_imgvEliminar: {
                Activity activity = getActivity();
                if (null != activity) {
                    consultarAUsuarioEliminarFotoDeGuia(activity,
                            getResources().getString(R.string.camara_msg_eliminar_title),
                            getResources().getString(R.string.camara_msg_eliminar_contenido),
                            getResources().getString(R.string.btn_aceptar_texto),
                            getResources().getString(R.string.btn_cancelar_texto));


                }
                break;
            }
        }
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        Log.i(TAG, "setAutoFlash: ");
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    //static
    private class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        private Context context;



        public ImageSaver(Image image, File file) {
            Log.i(TAG, "ImageSaver: ");
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
                Log.i(TAG, "run: Se grabo los bytes en el archivo de la foto");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                Log.i(TAG, "run: Se cerro el buffer");
                if (null != output) {
                    try {
                        output.close();
                        output.close();
                        Log.i(TAG, "run: Se cerro el fileOutputStream");

                        HandlerThread handlerThread= new HandlerThread("imagenGuia");
                        handlerThread.start();
                        Handler handler = new Handler(handlerThread.getLooper());

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Bitmap bitmap = null;
                                //Bitmap bitmap2 = null;
                                try {
                                    //bitmap = picasso.with(appContext).load(url).get();
                                    bitmap = Picasso.with(mContext).load(new File(mFile.getPath())).get();
                                    //bitmap= Picasso.with(mContext).load(new File(mFile.getPath())).resize(1280,960).get();
                                    File mFile2 = new File(getActivity().getExternalFilesDir(null), mFile.getName());
                                    FileOutputStream out = new FileOutputStream(mFile2);
                                    bitmap.compress(Bitmap.CompressFormat.JPEG,85, out);
                                    out.flush();
                                    out.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } finally {
                                    if (bitmap != null) {
                                        //do whatever you wanna do with the picture.
                                        //for me it was using my own cache
                                        //imageCaching.cacheImage(imageId, bitmap);ok perfectok
                                        final Bitmap finalBitmap = bitmap;
                                        mInstanceActivity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                //ocultar textureView y FrameTomarFoto
                                                mTextureView.setVisibility(View.GONE);
                                                mFrameLayoutTomarFoto.setVisibility(View.GONE);
                                                //mostrar imagen tomada, y opciones aceptar y cancelar
                                                //mLLayoutImageViewImagenPrevia.setVisibility(View.VISIBLE);
                                                mRlayoutContenedor.setVisibility(View.VISIBLE);
                                                mImageViewImagenPrevia.setVisibility(View.VISIBLE);
                                                mRLayoutAceptarCancelar.setVisibility(View.VISIBLE);
                                                //mImageViewImagenPrevia.setImageBitmap(finalBitmap);
                                                mImageViewImagenPrevia.setImage(ImageSource.bitmap(finalBitmap).dimensions(960,1280));
                                                //colocar a 1 para controlar que no levante nuevamente la captura de camara.
                                                IMG_TOMADA_VISTA_PREVIA=1;
                                                closeCamera();
                                                stopBackgroundThread();
                                            }
                                        });


                                    }
                                }
                            }
                        });


                        /*Picasso.with(mContext).load(new File(mFile.getPath()))
                                .into(new Target() {
                                    @Override
                                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {

                                    }

                                    @Override
                                    public void onBitmapFailed(Drawable errorDrawable) {

                                    }

                                    @Override
                                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                                        mImageViewImagenPrevia.setVisibility(View.VISIBLE);
                                        mImageViewImagenPrevia.setImageDrawable(placeHolderDrawable);
                                    }
                                });
*/


                        /* Log.i(TAG, "onCaptureCompleted: " + mFile.getPath());
                            Bitmap myBitmap = BitmapFactory.decodeFile(mFile.getPath());
                            mImageViewImagenPrevia.setImageBitmap(myBitmap);*/
                        /*mImageViewImagenPrevia.setVisibility(View.VISIBLE);*/

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final android.app.Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FragmentCompat.requestPermissions(parent,
                                    new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }

    private void confirmarFoto(){

        Activity activity = getActivity();
        if (null != activity) {
            Log.i(TAG, "confirmarFoto: ");
            /*File forigen = new File(getActivity().getExternalFilesDir(null), NRO_GUIA + "__1704251735" + ".jpg");
            File fdestino=new File(getActivity().getExternalFilesDir(null).getParent().toString() + "/" + nombreCarpetaImagenes, NRO_GUIA + "__1704251735"  + ".jpg");*/
            Calendar c = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            String strDate = sdf.format(c.getTime());

            File forigen = new File(getActivity().getExternalFilesDir(null), mFile.getName());
            File fdestino=new File(getActivity().getExternalFilesDir(null).getParent().toString() + "/" + nombreCarpetaImagenes, mFile.getName());
            File fdestinoGaleriaImagenes=new File(Environment.getExternalStorageDirectory() + "/DCIM/" + nombreCarpetaImagenesDCIM + "/" +
                    strDate + "/", mFile.getName());

            int resultado= 0;
            try {
                resultado = copiarImagenACarpetaImagenes(forigen, fdestino);
                if (resultado==1) {
                    Log.i(TAG, "confirmarFoto: " + "copiarImagenACarpetaImagenes");
                    eliminarImagenDeCarpetaTemporal(fdestinoGaleriaImagenes);// primero elimino
                    int resultadoCopiaAGaleriaImagenes=copiarImagenACarpetaImagenes(forigen, fdestinoGaleriaImagenes);//inserto nueva imagen tomada
                    if (eliminarImagenDeCarpetaTemporal(forigen)==1){
                        Log.i(TAG, "confirmarFoto: " + "Exito en Copia de Imagen");
                        elegirSalir_Or_TomarFotoDeGuiaSinFoto(NRO_GUIA, LISTA_GUIAS);
                    }else{
                        new AlertDialog.Builder(activity).setMessage("No se pudo eliminar la imagen temporal comunicarse con sistemas")
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    }

                }else{
                    new AlertDialog.Builder(activity).setMessage("No se pudo guardar la imagen comunicarse con sistemas")
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void consultarAUsuarioEliminarFotoDeGuia(Activity activity, String tituloMensaje, String contenidoMensaje, String textoDelBotonPositivo, String textoDelBotonNegativo) {

        new AlertDialog.Builder(activity)
                .setTitle(tituloMensaje)
                .setMessage(contenidoMensaje)
                .setPositiveButton(textoDelBotonPositivo, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        recapturarTomaDeFoto();
                    }
                })
                .setNegativeButton(textoDelBotonNegativo, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //nada
                    }
                })
                .show();
    }

    public void recapturarTomaDeFoto() {

        File fdelete = new File(getActivity().getExternalFilesDir(null), mFile.getName());
        try {
            if (eliminarImagenDeCarpetaTemporal(fdelete)==1){
                mostrarCamara();
            }else{
                Activity activity = getActivity();
                new AlertDialog.Builder(activity)
                        .setMessage("No se pudo guardar la imagen comunicarse con sistemas")
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void mostrarCamara() {
        Log.i(TAG, "mostrarCamara: ");


        /*mInstanceActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {*/
        //colocar a 1 para controlar que no levante nuevamente la captura de camara.
        IMG_TOMADA_VISTA_PREVIA=0;
        //ocultar textureView y FrameTomarFoto
        mTextureView.setVisibility(View.VISIBLE);
        mFrameLayoutTomarFoto.setVisibility(View.VISIBLE);
        //mostrar imagen tomada, y opciones aceptar y cancelar
        mRlayoutContenedor.setVisibility(View.GONE);
        mImageViewImagenPrevia.setVisibility(View.GONE);
        mRLayoutAceptarCancelar.setVisibility(View.GONE);
        //mImageViewImagenPrevia.setImageBitmap(null);
        mImageViewImagenPrevia.setImage(ImageSource.resource(R.drawable.preview_transparente));

        startBackgroundThread();

        Log.i(TAG, "onResume: IMG_TOMADA_VISTA_PREVIA " + Integer.toString(IMG_TOMADA_VISTA_PREVIA));

        if (IMG_TOMADA_VISTA_PREVIA==0){ // si no esta en la vista previa de la i

            // When the screen is turned off and turned back on, the SurfaceTexture is already
            // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
            // a camera and start preview from here (otherwise, we wait until the surface is ready in
            // the SurfaceTextureListener).
            if (mTextureView.isAvailable()) {
                Log.i(TAG, "onResume: " + "mTextureView available");
                openCamera(mTextureView.getWidth(), mTextureView.getHeight());
            } else {
                Log.i(TAG, "onResume: " + "mTextureView unvailable");
                mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
            }
        }
        //}
        //});

    }

    public static int copiarImagenACarpetaImagenes(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists()) {
            Log.i(TAG, "copiarImagenACarpetaImagenes: crear caperta imagenes");
            destFile.getParentFile().mkdirs();

        }
        if (!destFile.exists()) {
            Log.i(TAG, "copiarImagenACarpetaImagenes: crear el file sino existe");
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
            Log.i(TAG, "copyFile: " + "try");
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
            Log.i(TAG, "copyFile: " + "finally");
            return 1;

        }

    }

    public static int eliminarImagenDeCarpetaTemporal(File file) throws IOException {
        int rpta=0;
        if (file.exists()) {
            if (file.delete()) {
                rpta=1;
                Log.i(TAG, "eliminarImagenDeCarpetaTemporal: "  + " se elimino");
            } else {
                rpta=0;
                Log.i(TAG, "eliminarImagenDeCarpetaTemporal: "  + " no se elimino");
            }
        }
        return rpta;
    }

    public void elegirSalir_Or_TomarFotoDeGuiaSinFoto(String mNroGuia, ArrayList<HashMap<String, String>> listaGuias){

        Log.i(TAG, "elegirSalir_Or_TomarFotoDeGuiaSinFoto: ");

        int guiaPosicionLista=getPosicionGuiaEnLista(mNroGuia,listaGuias);
        TOMA_FOTO_CANTIDAD=listaGuias.get(guiaPosicionLista).get("toma_foto_cantidad").toString();
        int guiaCantidadFotosExistentes=getCantidadFotosExistentesByGuia(mNroGuia,Integer.parseInt(TOMA_FOTO_CANTIDAD));
        TOMA_FOTO_CANTIDAD_EXISTENTE=Integer.toString(guiaCantidadFotosExistentes);
        Log.i(TAG, "elegirSalir_Or_TomarFotoDeGuiaSinFoto: cantidad de fotos " + guiaCantidadFotosExistentes);
        if (guiaCantidadFotosExistentes<Integer.parseInt(TOMA_FOTO_CANTIDAD)){
            NRO_GUIA = mNroGuia;
            setDatosTomaFoto();
            mostrarCamara();

        }else{//buscar guias multiples sin foto
            String nroGuia=getGuiaSinFoto(mNroGuia, listaGuias);
            if (!nroGuia.equals("")){ //diferente de vacio
                //seteamos los datos de la guia para toma de foto
                NRO_GUIA = nroGuia;
                guiaPosicionLista=getPosicionGuiaEnLista(nroGuia,listaGuias);
                TOMA_FOTO_CANTIDAD=listaGuias.get(guiaPosicionLista).get("toma_foto_cantidad").toString();
                guiaCantidadFotosExistentes=getCantidadFotosExistentesByGuia(nroGuia,Integer.parseInt(TOMA_FOTO_CANTIDAD));
                TOMA_FOTO_CANTIDAD_EXISTENTE=Integer.toString(guiaCantidadFotosExistentes);
                setDatosTomaFoto();
                mostrarCamara();
            }else{
                closeCamera();
                stopBackgroundThread();
                getActivity().finish();
            }
        }

    }

    public void setDatosTomaFoto(){
        String textoGuia="";
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyyHHmmss");
        String strDate = sdf.format(c.getTime());

        if (Integer.parseInt(TOMA_FOTO_CANTIDAD)>1){ //solo para guias que tienen mas de 1 foto
            textoGuia= NRO_GUIA + " (" +  Integer.toString(Integer.parseInt(TOMA_FOTO_CANTIDAD_EXISTENTE) +1) + "/" + TOMA_FOTO_CANTIDAD + ")" ;
            mFile = new File(getActivity().getExternalFilesDir(null), NRO_GUIA + "__" + strDate + ".jpg");
        }else {
            textoGuia= NRO_GUIA;
            if (MOTIVADO.equals("NO")){ // no es Motivado
                mFile = new File(getActivity().getExternalFilesDir(null), NRO_GUIA + ".jpg");
            }else{
                mFile = new File(getActivity().getExternalFilesDir(null), NRO_GUIA + "__MOT" + strDate + ".jpg");
            }
        }

        Log.i(TAG, "setDatosTomaFoto: " + mFile.getName());

        txtvGuia.setText(textoGuia);
    }
    public int getCantidadFotosExistentesByGuia(String mNroGuia, int mTomaFotoCantidad){

        int tomaFotoCantidadExistente=0;

        //verifica cuantas imagenes tiene la guia
        if (mTomaFotoCantidad>1){
            String rutaCarpetaImagenes=getActivity().getExternalFilesDir(null).getParent().toString() + "/" + nombreCarpetaImagenes  ;

            File carpetaImagenes = new File(rutaCarpetaImagenes);

            File[] files = carpetaImagenes.listFiles();

            if (files!=null){

                String filename = "";
                for (File file : files) {

                    filename = file.getName();
                    int positionCaracterFoto=filename.indexOf("__");
                    int positionCaracterAdjunto=filename.indexOf("__ADJ");// el archivo de la foto adjunta tiene este formato
                    // Log.i(TAG, "getCantidadFotosExistentesByGuia: cantidad adjuntos " + positionCaracterAdjunto);
                    //Log.i(TAG, "getCantidadFotosExistentesByGuia: cantidad fotos " + positionCaracterFoto);
                    if (positionCaracterFoto>0 && positionCaracterAdjunto==-1){
                        String nroGuiaExtraida= filename.substring(0,positionCaracterFoto);
                        if (mNroGuia.equals(nroGuiaExtraida)){
                            tomaFotoCantidadExistente++;
                            Log.i(TAG, "getCantidadFotosExistentesByGuia: " + tomaFotoCantidadExistente);
                        }
                    }
                }
            }
        }else{ //cuando es 1
            String ruta=getActivity().getExternalFilesDir(null).getParent().toString() + "/" + nombreCarpetaImagenes + "/" + mNroGuia  + ".jpg";
            File fFile=new File(ruta);
            try {
                if (existeImagenByRutaFile(fFile)==1){
                    tomaFotoCantidadExistente++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (MOTIVADO.equals("SI")){
            int cantidadFotosMotivo=getCantidadFotosDeMotivoExistentesByGuia(mNroGuia);
            if (cantidadFotosMotivo>0){
                tomaFotoCantidadExistente++;
            }
        }

        return tomaFotoCantidadExistente;

    }

    public int getCantidadFotosDeMotivoExistentesByGuia(String mNroGuia){

        int tomaFotoCantidadExistente=0;

        String rutaCarpetaImagenes=getActivity().getExternalFilesDir(null).getParent().toString() + "/" + nombreCarpetaImagenes  ;

        File carpetaImagenes = new File(rutaCarpetaImagenes);

        File[] files = carpetaImagenes.listFiles();

        if (files!=null){

            String filename = "";
            for (File file : files) {

                filename = file.getName();
                int positionCaracterFoto=filename.indexOf("__MOT"); //MOT indica Motivado
                if (positionCaracterFoto>0 ){
                    String nroGuiaExtraida= filename.substring(0,positionCaracterFoto);
                    if (mNroGuia.equals(nroGuiaExtraida)){
                        tomaFotoCantidadExistente++;
                    }
                }
            }
        }
        return tomaFotoCantidadExistente;

    }

    public String getNombreFotoMotivoExistenteByGuia(String mNroGuia){

        String nombreImagenAdjunta="";

        //verifica cuantas imagenes tiene la guia
        String rutaCarpetaImagenes=getActivity().getExternalFilesDir(null).getParent().toString() + "/" + nombreCarpetaImagenes  ;

        File carpetaImagenes = new File(rutaCarpetaImagenes);

        File[] files = carpetaImagenes.listFiles();

        if (files!=null){

            String filename = "";
            for (File file : files) {

                filename = file.getName();
                int positionCaracterFoto=filename.indexOf("__MOT"); //formato de nombre de imagen adjunta
                if (positionCaracterFoto>0){
                    String nroGuiaExtraida= filename.substring(0,positionCaracterFoto);
                    if (mNroGuia.equals(nroGuiaExtraida)){
                        nombreImagenAdjunta=filename;
                    }
                }
            }
        }

        return nombreImagenAdjunta;

    }
    public String getGuiaSinFoto(String mNroGuia, ArrayList<HashMap<String, String>> listaGuias){
        //encuentra alguna guia sin foto adelante o atras de la guia que se ha tomado foto
        int posicion=getPosicionGuiaEnLista(mNroGuia,listaGuias);
        String nroGuiaSinFoto="";

        if (posicion>=0 && posicion<listaGuias.size()){

            int i=posicion+1;
            int encontroGuiaSinFoto=0;
            //busca un guia que no tenga foto hacia adelante
            while (i<listaGuias.size() && encontroGuiaSinFoto==0){
                String nroGuia=listaGuias.get(i).get("nro_guia").toString();
                int toma_foto_cantidad=Integer.parseInt(listaGuias.get(i).get("toma_foto_cantidad").toString());
                int guiaCantidadFotosExistentes=getCantidadFotosExistentesByGuia(nroGuia, toma_foto_cantidad);

                if (guiaCantidadFotosExistentes<toma_foto_cantidad){
                    encontroGuiaSinFoto=1;
                    nroGuiaSinFoto=nroGuia;
                }

                i++;
            }

            //busca un guia que no tenga foto hacia atras si es que no encontro una guia sin foto
            if (encontroGuiaSinFoto==0){
                i=posicion-1;
                while (i>=0 && encontroGuiaSinFoto==0){
                    String nroGuia=listaGuias.get(i).get("nro_guia").toString();
                    int toma_foto_cantidad=Integer.parseInt(listaGuias.get(i).get("toma_foto_cantidad").toString());
                    int guiaCantidadFotosExistentes=getCantidadFotosExistentesByGuia(nroGuia, toma_foto_cantidad);
                    if (guiaCantidadFotosExistentes<toma_foto_cantidad){
                        encontroGuiaSinFoto=1;
                        nroGuiaSinFoto=nroGuia;
                    }
                    i--;
                }
            }
        }else{
            mostrarMensaje("No se pudo encontrar la guia a proponer, comunicarse con Sistemas");
        }
        Log.i(TAG, "getGuiaSinFoto: " + nroGuiaSinFoto);
        return nroGuiaSinFoto;

    }

    public int getPosicionGuiaEnLista(String mNroGuia, ArrayList<HashMap<String, String>> listaGuias ){

        int posicion=-1;
        int i=0;
        int encontroGuia=0;
        String nroGuiaLista="";

        while (i<listaGuias.size() && encontroGuia==0){
            nroGuiaLista=listaGuias.get(i).get("nro_guia").toString();
            if (mNroGuia.equals(nroGuiaLista)){
                encontroGuia=1;
                posicion=i;
            }
            i++;
        }

        return posicion;
    }

    public static int existeImagenByRutaFile(File file) throws IOException {
        int rpta=0;
        if (file.exists()) {
            rpta = 1;
        }else{
            rpta=0;
        }
        return rpta;
    }
    public void mostrarMensaje(String mMensaje){

        new AlertDialog.Builder(getActivity()).setMessage(mMensaje)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
