package com.camera2.cibertec.camara2;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jcollantes on 13/02/2018.
 */

public class Splash extends AppCompatActivity {

    private String [] permits = new String[]{
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.CAMERA
    };

    public static final int MULTIPLE_PERMISSIONS = 10;
    private int REQUEST_PERMISSION_SETTING=101;
    private Context  mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext=this.getApplication();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermissions(mContext)){
            iniciarApp(mContext);
        }

    }

    private void iniciarApp(final Context context){

        //validar si usuario esta en la BD
        Thread splashHilo = new Thread() {
            public void run() {

                try {
                    // Thread will sleep for 5 seconds
                    //sleep(1*1000);

                        Intent intent = new Intent(context, RegistroEntrega.class);
                        startActivity(intent);
                        finish();


                } catch (Exception e) {
                }
            }
        };
        // start thread
        splashHilo.start();

    }
    private  boolean checkPermissions(Context context) {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();

        for (String p:permits) {
            result = ContextCompat.checkSelfPermission(context,p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),MULTIPLE_PERMISSIONS );
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {

                int sumaRespuestas = 0; // acumulará el valor de todas las respuestas
                if (grantResults.length > 0) {

                    boolean[] arrayShowRationale=new boolean[grantResults.length];
                    boolean resultadoRationale=false;
                    for (int i = 0; i < grantResults.length; i++) {
                        Log.i("Permisos", "onRequestPermissionsResult: " + i + " " + permissions[i].toString());
                        Log.i("grantResults", "onRequestPermissionsResult : resultado   " + i + "  " + grantResults[i] + " " + PackageManager.PERMISSION_GRANTED);

                        sumaRespuestas = sumaRespuestas + grantResults[i];

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            arrayShowRationale[i] = shouldShowRequestPermissionRationale(permissions[i].toString());
                            resultadoRationale=(resultadoRationale || arrayShowRationale[i]); //acumula resultados del rationale
                            Log.i("Permisos", "onRequestPermissionsResult: " + i + " " + arrayShowRationale[i]);
                        }
                    }
                    if (sumaRespuestas == 0) { //tiene todos los permisos solicitados
                        iniciarApp(mContext);
                    } else { //hubo o rechazo simple o rechazo con no recuerdo
                        if(resultadoRationale){  //al menos uno tuvo rechazo simple
                            checkPermissions(mContext);
                        }else{                      //rechazo con no recuerdo

                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                            finish();

                        }
                    }
                }
                return;
            }
        }
    }
}

