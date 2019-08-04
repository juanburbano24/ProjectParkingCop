package com.camera2.cibertec.camara2;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CameraPreview extends Activity {

    private static final String TAG = "CamaraPreview";


    //private static ImageView imgvFoto;
    private static SubsamplingScaleImageView imgvFoto;
    private static RelativeLayout mRlayoutContenedor;
    private static TextView txtvGuia;
    private static RelativeLayout flayout;
    private static LinearLayout linearLayoutConfirmar;
    private static LinearLayout linearLayoutEliminar;
    private static String nombreCarpetaImagenes;
    private static String nombreCarpetaImagenesDCIM;
    private static String entregaNombreFotoExistente;
    private static String nombreImagenAdjunta;//cuando se adjunta una imagen deberá tomar un nombre

    private static File FILE_IMAGEN_PREVIA;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart: ");

        String entregaNroGuia= getIntent().getStringExtra("EntregaNroGuia");
        entregaNombreFotoExistente= getIntent().getStringExtra("EntregaNombreFotoExistente");
        String imagePath=getIntent().getStringExtra("EntregaRutaImagen");

        imgvFoto=(SubsamplingScaleImageView)findViewById(R.id.camara_preview_ImgvImagenPrevia);
        mRlayoutContenedor=(RelativeLayout) findViewById(R.id.camara_preview_rlayoutContenedor);
        txtvGuia= (TextView)findViewById(R.id.camara_preview_txtNro_guia);
        txtvGuia.setText(entregaNroGuia);
        flayout =(RelativeLayout)findViewById(R.id.camara_preview_FrameLayoutMostrarFoto);
        linearLayoutEliminar=(LinearLayout)findViewById(R.id.camara_preview_layoutEliminar);
        linearLayoutConfirmar=(LinearLayout)findViewById(R.id.camara_preview_layoutConfirmar);

        findViewById(R.id.camara_preview_layoutRegresar).setOnClickListener(new HandleClick());
        findViewById(R.id.camara_preview_imgvRegresar).setOnClickListener(new HandleClick());
        findViewById(R.id.camara_preview_layoutEliminar).setOnClickListener(new HandleClick());
        findViewById(R.id.camara_preview_imgvEliminar).setOnClickListener(new HandleClick());
        findViewById(R.id.camara_preview_layoutConfirmar).setOnClickListener(new HandleClick());
        findViewById(R.id.camara_preview_imgvConfirmar).setOnClickListener(new HandleClick());

        nombreCarpetaImagenes=getResources().getString(R.string.camara_nombre_carpeta_imagenes);
        nombreCarpetaImagenesDCIM=getResources().getString(R.string.camara_nombre_carpeta_imagenes_dcim);

        //caso se adjunta foto
        //nombre del archivo a adjuntar
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyyHHmmss");
        String strDate = sdf.format(c.getTime());
        nombreImagenAdjunta =  entregaNroGuia + "__" + "ADJ" + strDate + ".jpg"; // se coloca ADJ para saber que es un adjunto

        if (imagePath==null){//es un previo de una foto , la imagen no es adjunta por tanto no tiene ruta
            linearLayoutEliminar.setVisibility(View.VISIBLE);
            linearLayoutConfirmar.setVisibility(View.GONE);
            FILE_IMAGEN_PREVIA=new File(getExternalFilesDir(null).getParent().toString() + "/" + nombreCarpetaImagenes, entregaNombreFotoExistente);
        }else{ //imagen adjunta con ruta
            linearLayoutConfirmar.setVisibility(View.VISIBLE);
            linearLayoutEliminar.setVisibility(View.GONE);
            FILE_IMAGEN_PREVIA=new File(imagePath);
        }


        if (FILE_IMAGEN_PREVIA.exists()) {
            mostrarImagen(imgvFoto, FILE_IMAGEN_PREVIA);
        }else{

        }
    }

    private void mostrarImagen(final SubsamplingScaleImageView mImageView , final File mFile){

        HandlerThread handlerThread= new HandlerThread("mostrarImagen");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());

        handler.post(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = null;
                try {
                    bitmap = Picasso.with(getApplicationContext()).load(new File(mFile.getPath())).get();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (bitmap != null) {
                        final Bitmap finalBitmap = bitmap;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                mRlayoutContenedor.setVisibility(View.VISIBLE);
                                imgvFoto.setVisibility(View.VISIBLE);
                                //mImageView.setVisibility(View.VISIBLE);
                                flayout.setVisibility(View.VISIBLE);
                                mImageView.setImage(ImageSource.bitmap(finalBitmap));
                                //mImageView.setImageBitmap(finalBitmap);

                            }
                        });
                    }
                }
            }
        });

    }

    private class HandleClick implements View.OnClickListener {
        public void onClick(final View view) {
            switch(view.getId()) {
                case R.id.camara_preview_layoutRegresar:
                    finish();
                    break;
                case R.id.camara_preview_imgvRegresar:
                    finish();
                    break;
                case R.id.camara_preview_layoutConfirmar:
                    confirmarFoto();
                    break;
                case R.id.camara_preview_imgvConfirmar:
                    confirmarFoto();
                    break;
                case R.id.camara_preview_layoutEliminar:
                    Log.i(TAG, "onClick: " + "Elimina desde Layout");
                    consultarAUsuarioEliminarFotoDeGuia(
                            getResources().getString(R.string.camara_msg_eliminar_title),
                            getResources().getString(R.string.camara_msg_eliminar_contenido),
                            getResources().getString(R.string.btn_aceptar_texto),
                            getResources().getString(R.string.btn_cancelar_texto));
                    break;
                case R.id.camara_preview_imgvEliminar:
                    Log.i(TAG, "onClick: " + "Elimina desde Imagen");
                    consultarAUsuarioEliminarFotoDeGuia(
                            getResources().getString(R.string.camara_msg_eliminar_title),
                            getResources().getString(R.string.camara_msg_eliminar_contenido),
                            getResources().getString(R.string.btn_aceptar_texto),
                            getResources().getString(R.string.btn_cancelar_texto));
                    break;
            }
        }
    }

    private void consultarAUsuarioEliminarFotoDeGuia( String tituloMensaje, String contenidoMensaje, String textoDelBotonPositivo, String textoDelBotonNegativo) {

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.MyAlertDialogTheme);
        builder.setMessage(contenidoMensaje)

                .setTitle(tituloMensaje);

        builder.setPositiveButton(textoDelBotonPositivo, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                eliminarFoto(txtvGuia.getText().toString());

            }
        });

        builder.setNegativeButton(textoDelBotonNegativo, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
        android.support.v7.app.AlertDialog dialog = builder.create();

        dialog.show();

    }

    public void eliminarFoto(String xnroGuia) {

        //File fdelete = new File(getExternalFilesDir(null), xnroGuia  + ".jpg");


        File fdelete=new File(getExternalFilesDir(null).getParent().toString() + "/" + nombreCarpetaImagenes, entregaNombreFotoExistente);
        try {
            if (eliminarImagenDeCarpetaTemporal(fdelete)==1){
                mostrarMensajeExito( getResources().getString(R.string.captura_preview_msg_informacion_title), getResources().getString(R.string.captura_preview_msg_informacion_exito_contenido), getResources().getString(R.string.msg_informacion_boton_aceptar_texto));
            }else{
                mostrarMensaje( getResources().getString(R.string.msg_informacion_title), getResources().getString(R.string.captura_preview_msg_informacion_error_contenido), getResources().getString(R.string.msg_informacion_boton_aceptar_texto));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void confirmarFoto(){

        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        String strDate = sdf.format(c.getTime());

        File fdestino=new File(getExternalFilesDir(null).getParent().toString() + "/" + nombreCarpetaImagenes, nombreImagenAdjunta);
        File fdestinoGaleriaImagenes=new File(Environment.getExternalStorageDirectory() + "/DCIM/" + nombreCarpetaImagenesDCIM + "/" +
                strDate + "/", nombreImagenAdjunta);

        int resultado= 0;
        try {
            resultado = copiarImagenACarpetaImagenes(FILE_IMAGEN_PREVIA, fdestino);
            if (resultado==1) {
                copiarImagenACarpetaImagenes(FILE_IMAGEN_PREVIA, fdestinoGaleriaImagenes);//inserto nueva imagen tomada
                finish();

            }else{
                mostrarMensaje( getResources().getString(R.string.msg_informacion_title), getResources().getString(R.string.captura_preview_msg_informacion_error_confirmar_contenido), getResources().getString(R.string.msg_informacion_boton_aceptar_texto));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int eliminarImagenDeCarpetaTemporal(File file) throws IOException {
        int rpta=0;
        if (file.exists()) {
            if (file.delete()) {
                rpta=1;
            } else {
                rpta=0;
            }
        }
        return rpta;
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

    private void mostrarMensajeExito( String tituloMensaje, String contenidoMensaje, String textoDelBoton) {

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.MyAlertDialogTheme);
        builder.setTitle(tituloMensaje)
                .setMessage(contenidoMensaje);

        builder.setPositiveButton(textoDelBoton, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.setCancelable(false);
        android.support.v7.app.AlertDialog dialog = builder.create();

        dialog.show();

    }

    private void mostrarMensaje(String tituloMensaje, String contenidoMensaje, String textoDelBoton) {

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.MyAlertDialogTheme);
        builder.setTitle(tituloMensaje)
                .setMessage(contenidoMensaje);

        builder.setPositiveButton(textoDelBoton, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
        builder.setCancelable(false);
        android.support.v7.app.AlertDialog dialog = builder.create();

        dialog.show();

    }
}
