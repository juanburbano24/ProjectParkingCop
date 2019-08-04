package com.camera2.cibertec.camara2;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class Camera extends Activity {

    private static final String TAG = "Camara";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        String entregaNroGuia= getIntent().getStringExtra("RegistroEntrega_NroGuia");
        String entregaTomaFotoCantidad= getIntent().getStringExtra("RegistroEntrega_TomaFotoCantidad");
        String entregaTomaFotoCantidadExistente= getIntent().getStringExtra("RegistroEntrega_TomaFotoCantidadExistente");
        String entregaMotivada= getIntent().getStringExtra("RegistroEntrega_Motivada"); //si es motivo sera "SI"


        ArrayList<HashMap<String, Long>> listaGuias=(ArrayList<HashMap<String, Long>>)getIntent().getSerializableExtra("RegistroEntrega_listaGuias");

        Log.i(TAG, "onCreate: " + " Guia " + listaGuias.toString());

        if (null == savedInstanceState) {
            CameraFragment cameraFragment=CameraFragment.newInstance();
            Bundle args = new Bundle();

            args.putString(cameraFragment.ARG_NRO_GUIA, entregaNroGuia );
            args.putString(cameraFragment.ARG_TOMA_FOTO_CANTIDAD,entregaTomaFotoCantidad);
            args.putString(cameraFragment.ARG_TOMA_FOTO_CANTIDAD_EXISTENTE,entregaTomaFotoCantidadExistente);
            args.putSerializable(cameraFragment.ARG_LISTA_GUIAS,listaGuias);

            if (entregaMotivada==null){
                args.putString(cameraFragment.ARG_MOTIVADO,"NO");
            }else{
                args.putString(cameraFragment.ARG_MOTIVADO,entregaMotivada);
            }

            cameraFragment.setArguments(args);

            getFragmentManager().beginTransaction()
                    .replace(R.id.camera_container, cameraFragment)
                    .commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart: ");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause: ");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop: ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: ");

    }
}
