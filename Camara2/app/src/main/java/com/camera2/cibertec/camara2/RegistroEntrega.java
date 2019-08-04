package com.camera2.cibertec.camara2;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.camera2.cibertec.camara2.rest.ApiClient;
import com.camera2.cibertec.camara2.rest.RegistroRestService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistroEntrega extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener ,View.OnClickListener{

    private static final String TAG ="RegistroEntrega" ;

    String[] from;
    int[] to;
    ArrayList<HashMap<String, String>> listGuias;
    SimpleAdapter listGuiasAdapter;
    ListView listViewGuias;

    String solicitudAno="";
    Integer solicitudNro=0;
    String clienteNombre="";
    String clienteContacto="";

    ArrayList<HashMap<String, Long>> listaCodigoRegistrosInsertados;
    ArrayList<HashMap<String, Long>> listaCodigoRegistrosImagenesInsertadas; //guardaran en esta variable los codigos de los Registros insertados

    //Integer codigoRegistro2=20;

    private TextView contentTxt;
    private ImageButton imgBtn;

    private static String nombreCarpetaImagenes;

    //LOCATION
    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS/2 ;

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;
    LatLng mMyLocationLatLng;


    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    protected Boolean mRequestingLocationUpdates;

    /**
     * Time when the location was updated represented as a String.
     */
    protected String mLastUpdateTime;

    //FIN LOCATION

    final private int ACTIVITY_RESULT_ADJUNTAR_IMAGEN=1001;
    final private int ACTIVITY_RESULT_ADJUNTAR_IMAGEN_PREVIEW=1002;
    final private int ACTIVITY_RESULT_ESCANEAR_GENERAL=2001;
    private int POSICION_ITEM_LISTA_ADJUNTAR_IMAGEN; //cada vez que le da clic en la guia para adjuntar se guardará la posicion para luego utilizarla para en el activity result

    Button btnGuardar;
    private ProgressDialog progressDialog;
    private static Semaphore semaphoreRegistro = new Semaphore(1);
    private static Semaphore semaphoreImagenes = new Semaphore(1);

    Integer qRegistrosEntregaEnviados = 0;         // contador para mostrar el mensaje de finalizacion envios exitosos
    Integer qRegistrosEntregaNoEnviados = 0;       // contador para mostrar el mensaje de finalizacion envios no exitosos iran a pendientes
    Integer qRegistrosLlegadaMultipleEnviados = 0; // contador que controlorá el envio multiple de hora de llegada

    Integer FLG_FIRST_TIME_IN_ACTIVITY=0;             //controla si se ingresa por primera vez en el activity

    static final String STATE_FIRST_TIME_IN_ACTIVITY="state_first_time_in_activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_registro_entrega);

        setToolbar();
        setHandleClick();

        inicializarVariables();
        inicializarObjetos();
        ejecutarProcesosIniciales();
        buildGoogleApiClient();

    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        Log.i(TAG, "onStart: ");
        actualizaIconosCamaraEnListaGuias(listGuias);
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: ");
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause: ");
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (mGoogleApiClient.isConnected()) {
            Log.i(TAG, "onPause: stopLocationUpdate");
            stopLocationUpdates();
        }
    }
    @Override
    protected void onStop() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
            Log.i(TAG, "onStop: mGoogleApiClient desconectar");
        }
        super.onStop();
        Log.i(TAG, "onStop: ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: ");
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(STATE_FIRST_TIME_IN_ACTIVITY, FLG_FIRST_TIME_IN_ACTIVITY);

    }

    private void setToolbar(){
        Toolbar toolbarDetalleEntrega = (Toolbar) findViewById(R.id.toolbar_registro_entrega);
        this.setSupportActionBar(toolbarDetalleEntrega);
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Enable the Up button
        this.getSupportActionBar().setDisplayShowTitleEnabled(false);//no muestra el title por defecto
        ((TextView) this.findViewById(R.id.reg_entrega_toolbar_title)).setText(R.string.reg_entrega_title_entrega_texto);//set titulo
    }

    private void setHandleClick(){

        HandleClick  handleClick=new HandleClick();
        findViewById(R.id.reg_entrega_layoutAgregarGuia).setOnClickListener(handleClick);
        findViewById(R.id.reg_entrega_agregar).setOnClickListener(handleClick);

        findViewById(R.id.reg_entrega_layoutAgregarGuiaPorEscaner).setOnClickListener(handleClick);
        findViewById(R.id.reg_entrega_agregar_guia_por_escaner).setOnClickListener(handleClick);

        findViewById(R.id.reg_entrega_layoutAgregarGuiaPorLista).setOnClickListener(handleClick);
        findViewById(R.id.reg_entrega_agregar_guia_por_lista).setOnClickListener(handleClick);

        //findViewById(R.id.reg_entrega_aceptar).setOnClickListener(handleClick);
    }

    private void inicializarVariables(){
        //int codigoRegistro= getIntent().getIntExtra("codigoRegistro",0);
        listaCodigoRegistrosInsertados=(ArrayList<HashMap<String, Long>>)getIntent().getSerializableExtra("listaCodigoRegistrosInsertados");
        solicitudAno= getIntent().getStringExtra("solicitudAno");
        solicitudNro= getIntent().getIntExtra("solicitudNro", 0);
        nombreCarpetaImagenes=getResources().getString(R.string.camara_nombre_carpeta_imagenes);

        clienteNombre="Juan";
        clienteContacto="Perez";

        mRequestingLocationUpdates=true;

        POSICION_ITEM_LISTA_ADJUNTAR_IMAGEN=0;

        FLG_FIRST_TIME_IN_ACTIVITY=1;
    }

    private void inicializarObjetos(){

        final ProgressDialog progressDialog = new ProgressDialog(RegistroEntrega.this);

        setListaGuia(R.layout.adapter_registro_entrega, this, listaCodigoRegistrosInsertados);

        //llenarListaCombos();
        btnGuardar=findViewById(R.id.reg_entrega_btnGuardar);
        btnGuardar.setOnClickListener(this);


        final EditText etxtRecibido=(EditText)findViewById(R.id.reg_entrega_etxt_recibidoPor);
        etxtRecibido.setNextFocusDownId(R.id.reg_entrega_etxt_dni);
        etxtRecibido.setSelectAllOnFocus(true);
        etxtRecibido.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.clearFocus();
                view.requestFocus();
            }
        });
        etxtRecibido.setVisibility(View.GONE);

        EditText etxDni=(EditText)findViewById(R.id.reg_entrega_etxt_dni);
        etxDni.setNextFocusDownId(R.id.reg_entrega_etxt_observacion);
        etxDni.setVisibility(View.GONE);
        //spinner lugar de entrega
        final Spinner spinnerLugarEntrega=(Spinner) findViewById(R.id.reg_entrega_spinner_lugar);
        spinnerLugarEntrega.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {

            } // to close the onItemSelected
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });
        spinnerLugarEntrega.setVisibility(View.GONE);

    }

    private void ejecutarProcesosIniciales(){

        setDatosRegistroYaIngresado(listaCodigoRegistrosInsertados); //si viene del detalle que muestre los datos que existen
        calcularCantidadGuias();
        //setClickOnImgEliminar(getApplicationContext(),0);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){

            case R.id.reg_entrega_btnGuardar:

                if (verificaEntregaAceptar(this)==1){
                    registrarGuiasyCabecera(this,view);
                }
                break;

        }

    }

    private class HandleClick implements View.OnClickListener {
        public void onClick(final View view) {
            switch(view.getId()) {
                case R.id.reg_entrega_layoutAgregarGuia:
                    agregarGuiaEnLista(view);
                    break;
                case R.id.reg_entrega_agregar:
                    agregarGuiaEnLista(view);
                    break;
                case R.id.reg_entrega_layoutAgregarGuiaPorEscaner:
                    scanearCodigoBarra();
                    break;
                case R.id.reg_entrega_agregar_guia_por_escaner:
                    scanearCodigoBarra();
                    break;
                case R.id.reg_entrega_layoutAgregarGuiaPorLista:
                    mostrarListaGuias(view);
                    break;
                case R.id.reg_entrega_agregar_guia_por_lista:
                    mostrarListaGuias(view);
                    break;

            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        /*IntentResultScan scanningResult = IntentIntegratorScan.parseActivityResult(requestCode, resultCode, data);
        if (scanningResult != null) {

            String scanContent = scanningResult.getContents();
            contentTxt.setText(scanContent);

            String guiaScaneada=contentTxt.getText().toString();

            View view = this.getCurrentFocus();

            Integer existeMsg = 1;

            if (guiaScaneada.length()>0) {
                existeMsg = agregarGuiaEnLista(view);
            }

            if (guiaScaneada.length()>0 && existeMsg == 1) {
                scanearCodigoBarra();
            }
        }
        else{ //no ha pasado por el scaner
            //Toast toast = Toast.makeText(getApplicationContext(),
            //"No scan data received!", Toast.LENGTH_SHORT);
            //toast.show();
            //----------------------------------RECIBE LAS GUIAS AGREGADAS--------------------------------------
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == 1) {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        //if (data.getStringExtra("listaGuiasAgregadas") != null) { //esto se hace por  el boton de retroceso que no pasa los extras
                        Log.i(TAG, "onActivityResult: ");
                        ArrayList<HashMap<String, String>> listaGuiasPorAgregar= (ArrayList<HashMap<String, String>>) data.getSerializableExtra("listaGuiasAgregadas");
                        //listarArrayHashMap(listaGuiasPorAgregar);
                        agregarListaGuiasSeleccionadas(listaGuiasPorAgregar);
                        //}
                    }
                }
            }else{
                if (requestCode == ACTIVITY_RESULT_ADJUNTAR_IMAGEN) {
                    if (resultCode == Activity.RESULT_OK) {
                        if (data != null) {
                            Log.i(TAG, "onActivityResult:xxxxxxxx ");
                            mostrarImageGalleryPreview(data, POSICION_ITEM_LISTA_ADJUNTAR_IMAGEN);
                        }
                    }
                }
            }
        }*/
    }

    public void setListaGuia(Integer xlayout, final Context context, ArrayList<HashMap<String, Long>> listaCodigoRegistrosInsertados){

        from = new String[]{"solicitud_ano","solicitud_nro","nro_guia","opc_eliminar", "opc_adjuntar_imagen","opc_registrar_imagen",
                "opc_vista_previa","flg_registrar_imagen","toma_foto_cantidad"};
        to = new int[]{R.id.reg_entrega_txtSolicitudAno,R.id.reg_entrega_txtSolicitudNro,R.id.reg_entrega_nro_guia,
                R.id.reg_entrega_imgEliminar, R.id.reg_entrega_imgAdjuntarImagen, R.id.reg_entrega_imgRegistarImagen,
                R.id.reg_entrega_imgVistaPrevia, R.id.reg_entrega_flg_registrar_imagen, R.id.reg_entrega_toma_foto_cantidad};

        listGuias = new ArrayList<HashMap<String, String>>();

        if (listaCodigoRegistrosInsertados==null){ //si no hay codigo Registrados que solo inserte el de la guia


            HashMap<String, String> datosEnvio = new HashMap<String, String>();

            datosEnvio.put("solicitud_ano", solicitudAno);
            datosEnvio.put("solicitud_nro", String.valueOf(solicitudNro));
            datosEnvio.put("nro_guia", "GUIA00013");//solicitudAtencion.getSolicitudReferencia()
            datosEnvio.put("opc_eliminar", Integer.toString(R.drawable.cancelar_verde_12x12));

            //inicialmente seteo opciones camara en blanco
            datosEnvio.put("opc_adjuntar_imagen", Integer.toString(R.drawable.en_blanco_19x18));
            datosEnvio.put("opc_registrar_imagen", Integer.toString(R.drawable.en_blanco_19x18));
            datosEnvio.put("opc_vista_previa", Integer.toString(R.drawable.en_blanco_19x18));

            //datosEnvio.put("flg_registrar_imagen", solicitudAtencion.getRegistrarImagen());
            //datosEnvio.put("toma_foto_cantidad", Integer.toString(solicitudAtencion.getTomaFotoCantidad()));
            datosEnvio.put("flg_registrar_imagen", "1");
            datosEnvio.put("toma_foto_cantidad", Integer.toString(1));

            //listGuias.add(datosEnvio);

        }

        listGuiasAdapter = new SimpleAdapter(this.getApplicationContext(), listGuias, xlayout,from,to)
        {
            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {

                View itemView=super.getView(position, convertView, parent);

                //------------------------------------ADJUNTAR FOTO-----------------------------------------------
                //clic en la imagen adjuntar foto
                ImageView imgAdjuntarImagen= (ImageView)itemView.findViewById(R.id.reg_entrega_imgAdjuntarImagen);
                imgAdjuntarImagen.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setClickAdjuntarFoto(context,position);

                    }
                });

                //clic en el LinearLayout adjuntar foto
                LinearLayout layoutAdjuntarImagen= (LinearLayout)itemView.findViewById(R.id.reg_entrega_layoutAdjuntarImagen);
                layoutAdjuntarImagen.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setClickAdjuntarFoto(context,position);
                    }
                });

                //------------------------------------TOMAR FOTO-----------------------------------------------

                //clic en la imagen tomar foto
                ImageView imgRegistrarImagen= (ImageView)itemView.findViewById(R.id.reg_entrega_imgRegistarImagen);
                imgRegistrarImagen.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setClickOnTomarFoto(context,position);
                    }
                });

                //clic en el LinearLayout tomar foto
                LinearLayout layoutRegistrarImagen= (LinearLayout)itemView.findViewById(R.id.reg_entrega_layoutRegistarImagen);
                layoutRegistrarImagen.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setClickOnTomarFoto(context,position);
                    }
                });
                //------------------------------------PREVISUALIZAR IMAGEN-----------------------------------------------
                ImageView imgImagenPrevia= (ImageView)itemView.findViewById(R.id.reg_entrega_imgVistaPrevia);
                imgImagenPrevia.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setClickOnImagePreview(position);
                    }
                });

                //clic en el LinearLayout previsualizar imagen
                LinearLayout layoutImagenPrevia= (LinearLayout)itemView.findViewById(R.id.reg_entrega_layoutVistaPrevia);
                layoutImagenPrevia.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setClickOnImagePreview(position);
                    }
                });
                //------------------------------------ELIMINAR GUIA DE LISTA----------------------------------------------
                ImageView imgEliminar= (ImageView)itemView.findViewById(R.id.reg_entrega_imgEliminar);
                imgEliminar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setClickOnImgEliminar(context, position);
                    }
                });

                //clic en el LinearLayout de eliminar guia
                LinearLayout layoutEliminarGuia= (LinearLayout)itemView.findViewById(R.id.reg_entrega_layoutEliminarGuia);
                layoutEliminarGuia.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setClickOnImgEliminar(context, position);
                    }
                });

                return itemView;
            }
        };

        if (listViewGuias==null){
            listViewGuias = (ListView) this.findViewById(R.id.reg_entrega_listaGuias);
        }

        listViewGuias.setAdapter(listGuiasAdapter);
    }

    private void setClickAdjuntarFoto(Context context, int positionItemLista){

        String nroGuia=listGuias.get(positionItemLista).get("nro_guia").toString();
        int cantidadAdjuntos=getCantidadAdjuntosExistentesByGuia(nroGuia);

        POSICION_ITEM_LISTA_ADJUNTAR_IMAGEN=positionItemLista; //setear la posicion del item seleccionado para adjuntar imagen

        if (cantidadAdjuntos<1) {

           /* Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent,	ACT_RESULT_ADJUNTAR_IMAGEN);*/

            // File System.
            final Intent galleryIntent = new Intent();
            galleryIntent.setType("image/*");
            galleryIntent.setAction(Intent.ACTION_PICK);

            // Chooser of file system options.
            final Intent chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.reg_entrega_titulo_texto_adjuntar_imagen));
            startActivityForResult(chooserIntent, ACTIVITY_RESULT_ADJUNTAR_IMAGEN);

        }else{
            mostrarMensaje(context, getResources().getString(R.string.msg_informacion_title),getResources().getString(R.string.reg_entrega_msg_validacion_existe_adjunto_contenido) , getResources().getString(R.string.msg_error_boton_aceptar_texto));
        }
    }

    private void setClickOnTomarFoto(Context context , int positionItemLista){
        String nroGuia=listGuias.get(positionItemLista).get("nro_guia").toString();
        int toma_foto_cantidad=1;//Integer.parseInt(listGuias.get(positionItemLista).get("toma_foto_cantidad").toString());

        try {
            if (toma_foto_cantidad==1){ //solo 1 foto por guia
                String ruta=getExternalFilesDir(null).getParent().toString() + "/" + nombreCarpetaImagenes + "/" + nroGuia  + ".jpg";
                File fFile=new File(ruta);

                if (existeImagenByRutaFile(fFile)==1){
                    mostrarMensaje(context, getResources().getString(R.string.msg_informacion_title),getResources().getString(R.string.reg_entrega_msg_validacion_existe_foto_contenido) , getResources().getString(R.string.msg_error_boton_aceptar_texto));
                }else{
                    Intent intent = new Intent(RegistroEntrega.this, Camera.class);
                    intent.putExtra("RegistroEntrega_listaGuias", listGuias);
                    intent.putExtra("RegistroEntrega_NroGuia", nroGuia);
                    intent.putExtra("RegistroEntrega_TomaFotoCantidad", Integer.toString(toma_foto_cantidad));
                    intent.putExtra("RegistroEntrega_TomaFotoCantidadExistente", "1");
                    startActivityForResult(intent, 1);
                }

            }else if(toma_foto_cantidad>1) {

                int tomaFotoCantidadExistente = getCantidadFotosExistentesByGuia(nroGuia, toma_foto_cantidad);

                //se comenta para que siempre permita tomar mas fotos
                //if (tomaFotoCantidadExistente<toma_foto_cantidad){
                Intent intent = new Intent(RegistroEntrega.this, Camera.class);
                intent.putExtra("RegistroEntrega_listaGuias", listGuias);
                intent.putExtra("RegistroEntrega_NroGuia", nroGuia);
                //se puede tomar mas fotos que la cantidad minima exigible, cuando es mayor que la cantidad minima debe cambiar el formato  5/4 por 5/5
                if (tomaFotoCantidadExistente < toma_foto_cantidad){
                    intent.putExtra("RegistroEntrega_TomaFotoCantidad", Integer.toString(toma_foto_cantidad));
                }else{
                    intent.putExtra("RegistroEntrega_TomaFotoCantidad", Integer.toString(tomaFotoCantidadExistente+1));
                }
                intent.putExtra("RegistroEntrega_TomaFotoCantidadExistente", Integer.toString(tomaFotoCantidadExistente));
                startActivityForResult(intent, 1);
                //}else{
                //    mostrarMensaje(context, getResources().getString(R.string.msg_informacion_title),getResources().getString(R.string.reg_entrega_msg_validacion_existen_fotos_contenido) , getResources().getString(R.string.msg_error_boton_aceptar_texto));
                //}


            }else{ //no se pudo obtener datos de cantidad de fotos x guia
                mostrarMensaje(context, getResources().getString(R.string.msg_informacion_title),getResources().getString(R.string.reg_entrega_msg_validacion_toma_foto_cantidad_contenido) , getResources().getString(R.string.msg_error_boton_aceptar_texto));
            }

            listGuiasAdapter.notifyDataSetChanged();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setClickOnImagePreview(int positionItemLista){

        String nroGuia=listGuias.get(positionItemLista).get("nro_guia").toString();
        int mTomaFotoCantidad=Integer.parseInt(listGuias.get(positionItemLista).get("toma_foto_cantidad").toString());
        String nombresFotosExistentesByGuia=getNombresFotosExistentesByGuia(nroGuia,mTomaFotoCantidad);

        if (!nombresFotosExistentesByGuia.equals("")){
            Intent intent = new Intent(RegistroEntrega.this, CameraPreview.class);
            intent.putExtra("EntregaNroGuia", nroGuia);
            intent.putExtra("EntregaNombreFotoExistente", nombresFotosExistentesByGuia);
            startActivityForResult(intent, 1);
        }else{
            //mostrarMensaje(context, getResources().getString(R.string.msg_informacion_title),getResources().getString(R.string.reg_entrega_msg_validacion_existe_foto_contenido) , getResources().getString(R.string.msg_error_boton_aceptar_texto));
        }
    }

    private void mostrarImageGalleryPreview(Intent data, int positionItemLista){

        String imagePath;
        String nroGuia=listGuias.get(positionItemLista).get("nro_guia").toString();
        String nombresFotosExistentesByGuia="";

        if (data == null) {
            return;
        }
        Uri selectedImageUri = data.getData();
        String[] filePathColumn = {MediaStore.Images.Media.DATA};

        Cursor cursor = getContentResolver().query(selectedImageUri, filePathColumn, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            imagePath = cursor.getString(columnIndex);

            cursor.close();

            Intent intent = new Intent(RegistroEntrega.this, CameraPreview.class);
            intent.putExtra("EntregaNroGuia", nroGuia);
            intent.putExtra("EntregaNombreFotoExistente", nombresFotosExistentesByGuia);
            intent.putExtra("EntregaRutaImagen", imagePath);
            startActivityForResult(intent, ACTIVITY_RESULT_ADJUNTAR_IMAGEN_PREVIEW);

        } else {

        }

        if (!nombresFotosExistentesByGuia.equals("")){
            Intent intent = new Intent(RegistroEntrega.this, CameraPreview.class);
            intent.putExtra("EntregaNroGuia", nroGuia);
            intent.putExtra("EntregaNombreFotoExistente", nombresFotosExistentesByGuia);
            startActivityForResult(intent, 1);
        }else{
            //mostrarMensaje(context, getResources().getString(R.string.msg_informacion_title),getResources().getString(R.string.reg_entrega_msg_validacion_existe_foto_contenido) , getResources().getString(R.string.msg_error_boton_aceptar_texto));
        }
    }
    private void setClickOnImgEliminar(Context context , final int positionItemLista){
        String nroGuia=listGuias.get(positionItemLista).get("nro_guia").toString();
        int mSolicitudNro=Integer.parseInt(listGuias.get(positionItemLista).get("solicitud_nro").toString());
        Log.i(TAG, "setClickOnImgEliminar: " + mSolicitudNro);

        String ruta=getExternalFilesDir(null).getParent().toString() + "/" + nombreCarpetaImagenes + "/" + nroGuia  + ".jpg";
        File fFile=new File(ruta);

        try {

            //if (mSolicitudNro==solicitudNro){
            //    mostrarMensaje(context, getResources().getString(R.string.msg_informacion_title),getResources().getString(R.string.reg_entrega_msg_eliminar_guia_origen_contenido) , getResources().getString(R.string.msg_error_boton_aceptar_texto));
            //}else{
                if (existeImagenByRutaFile(fFile)==1){
                    mostrarMensaje(context, getResources().getString(R.string.msg_informacion_title),getResources().getString(R.string.reg_entrega_msg_eliminar_guia_existe_foto_contenido) , getResources().getString(R.string.msg_error_boton_aceptar_texto));
                }else{
                    consultarEliminarGuia (context, positionItemLista);
                }
            //}

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void consultarEliminarGuia (Context context, final int positionItemLista){

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Desea eliminar la guia \"" + listGuias.get(positionItemLista).get("nro_guia").toString() + "\"?")
                .setTitle("Eliminar");

        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                listGuias.remove(positionItemLista);
                listGuiasAdapter.notifyDataSetChanged();
                calcularCantidadGuias();
            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void mostrarListaGuias(View view){

        int rpta=validarGuiasPorAgregar();

        if (rpta==1){
            Intent intent = new Intent(RegistroEntrega.this, MainActivity.class);

            intent.putExtra("RegistroEntrega_listaGuias", listGuias);//guias ya ingresadas
            intent.putExtra("RegistroEntrega_ClienteNombre", clienteNombre);
            intent.putExtra("RegistroEntrega_ClienteContacto", clienteContacto);
            startActivityForResult(intent, 1);
        }else{
            mostrarMensaje(view, getResources().getString(R.string.msg_informacion_title), getResources().getString(R.string.reg_entrega_msg_valida_mostrar_lista_guias_contenido), getResources().getString(R.string.btn_aceptar_texto));
        }

    }

    private int validarGuiasPorAgregar(){
        int rpta=1;


        return rpta;
    }

    public int agregarGuiaEnLista (View view){

        int resultado;

        EditText etxtGuia=(EditText)findViewById(R.id.reg_entrega_etxt_guia);
        String mNroGuia=etxtGuia.getText().toString().toUpperCase();

        if (mNroGuia.equals("")){
            resultado=0;
            mostrarMensaje(view, getResources().getString(R.string.msg_informacion_title), getResources().getString(R.string.reg_entrega_msg_sin_guia_a_ingresar_contenido), getResources().getString(R.string.btn_aceptar_texto));
        }else{

                resultado = validarIngresoGuia(view, mNroGuia);

                if (resultado==1){//paso validaciones
                    etxtGuia.setText("");
                    agregaDatosDeGuiaEnLista(solicitudAno, solicitudNro, mNroGuia,"1", 1);
                }

        }

        return resultado;
    }

    private int validarIngresoGuia(View view, String mNroGuia){
        int valida=0;

        if (existeElementoRepetidoListaGuia(listGuias, mNroGuia)==1){
            mostrarMensaje(view, getResources().getString(R.string.msg_informacion_title), getResources().getString(R.string.reg_entrega_msg_guia_a_ingresar_repetida_contenido), getResources().getString(R.string.btn_aceptar_texto));
        }
        else {
                valida=1;
        }

        return valida;
    }

    private void agregaDatosDeGuiaEnLista (String mSolicitudAno, int mSolicitudNro, String mNroGuia, String mFlgRegistrarImagen,
                                           int mToma_foto_cantidad){

        HashMap<String, String> guia = new HashMap<String, String>();
        guia.put("solicitud_ano", mSolicitudAno);
        guia.put("solicitud_nro", String.valueOf(mSolicitudNro));
        guia.put("nro_guia", mNroGuia);
        guia.put("opc_eliminar", Integer.toString(R.drawable.ic_eliminar));
        guia.put("opc_adjuntar_imagen", Integer.toString(R.drawable.ic_attach_file));
        guia.put("opc_registrar_imagen", Integer.toString(R.drawable.ic_agregar_foto));
        guia.put("opc_vista_previa", Integer.toString(R.drawable.preview_transparente));
        guia.put("flg_registrar_imagen",mFlgRegistrarImagen);
        guia.put("toma_foto_cantidad",Integer.toString(mToma_foto_cantidad));
        //guia.put("toma_foto_cantidad",Integer.toString(4));

        listGuias.add(guia);
        listGuiasAdapter.notifyDataSetChanged();

        ocultarSoftKeyboard();
        calcularCantidadGuias();
    }

    private void consultarAUsuarioAgregarGuiaSiNo(final String mSolicitudAno, final int mSolicitudNro,
                                                  final String mNroGuia, final String mFLG_Registrar_imagen,
                                                  final int mToma_foto_cantidad,
                                                  final View view, String tituloMensaje,
                                                  String contenidoMensaje, String textoDelBotonPositivo,
                                                  String textoDelBotonNegativo) {

        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext()); //, R.style.MyAlertDialogTheme
        builder.setMessage(contenidoMensaje)

                .setTitle(tituloMensaje);

        builder.setPositiveButton(textoDelBotonPositivo, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                EditText etxtGuia=(EditText)findViewById(R.id.reg_entrega_etxt_guia);
                etxtGuia.setText("");
                agregaDatosDeGuiaEnLista(mSolicitudAno, mSolicitudNro, mNroGuia, mFLG_Registrar_imagen, mToma_foto_cantidad);
            }
        });

        builder.setNegativeButton(textoDelBotonNegativo, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener(){

                                     @Override
                                     public void onShow(DialogInterface xdialog) {
                                         Button negative= dialog.getButton(dialog.BUTTON_NEGATIVE);
                                         negative.setFocusable(true);
                                         negative.setFocusableInTouchMode(true);
                                         negative.requestFocus();
                                     }
                                 }
        );
        dialog.show();

    }

    private void setDatosRegistroYaIngresado(ArrayList<HashMap<String, Long>> listaCodigoRegistro){

    }

    public int verificaEntregaAceptar(final Context context)  {

        int verifica=1;

        return verifica;

    }

    private void registrarGuiasyCabecera(Context context,View view) {



        if (listaCodigoRegistrosInsertados!=null) {

            if (listaCodigoRegistrosInsertados.size()>0){

                verificaEnvioRegistroEntrega(view);
            }else{
                mostrarMensaje(context, getResources().getString(R.string.msg_informacion_title), "No se obtuvo el número de Registro de Base de Datos interna, consulte con Sistemas",getResources().getString(R.string.msg_error_boton_aceptar_texto));
            }

        } else {
            mostrarMensaje(context, getResources().getString(R.string.msg_error_title), "No se registró ninguna Solicitud en la Base de Datos interna, consulte con Sistemas", getResources().getString(R.string.msg_error_boton_aceptar_texto));
        }

    };


    private void verificaEnvioRegistroEntrega(final View view) {

    }

    private void consultarAUsuarioEnvioRegistroEntregaSiNo(final View view, String tituloMensaje, String contenidoMensaje, String textoDelBotonPositivo, String textoDelBotonNegativo) {

        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext(), R.style.MyAlertDialogTheme);
        builder.setMessage(contenidoMensaje)
                //.setIcon(R.drawable.ic_action_save_24x24)
                .setTitle(tituloMensaje);

        builder.setPositiveButton(textoDelBotonPositivo, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                //progressDialog
                //progressDialog = new ProgressDialog(getApplicationContext());
                //progressDialog.setIndeterminate(true);
                //progressDialog.setMessage(getResources().getString(R.string.barra_progreso_texto));
                //progressDialog.show();
                //end progressDialog

                //restriccion hilo imagenes
                try {
                    semaphoreImagenes.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                new Thread(new Runnable() {
                    public void run() {


                        //==========================que espere el termino de envio de registros de hora de llegada y limpiar hora de llegada sqlite============================================
                        try {
                            Log.i(TAG, "onClick: acquire del registro guia");
                            semaphoreRegistro.acquire();
                            Log.i(TAG, "onClick: paso acquire del registro guia");
                        } catch (InterruptedException e) {
                            Log.i(TAG, "onClick: alternativa al acquire");
                            e.printStackTrace();
                        }


                        //=======================================Enviar registros de entrega================================================================
                        for (int i = 0; i < listaCodigoRegistrosInsertados.size(); i++) {
                            //try {
                            //Log.i(TAG, "run: " + "duerme por 5 sec " + i) ;
                            //Thread.currentThread().sleep(5000);
                            enviarRegistroEntrega(view, listaCodigoRegistrosInsertados.get(i).get("codigoRegistro"), listaCodigoRegistrosInsertados.size());
                            //Thread.currentThread().sleep(5000);
                            //} catch (InterruptedException e) {
                            //   e.printStackTrace();
                            //}

                        }
                        semaphoreRegistro.release();

                    }
                }).start();


                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "inicia hilo Imagenes");
                        Log.i(TAG, " acquire del envio de imagenes");

                        final Collection<RegistroRequest> listRegistroImagen = getListaRegistroImagenPorEnviar(view);

                        try {
                            semaphoreImagenes.acquire();
                            semaphoreImagenes.release();
                            Log.i(TAG, " release del envio de imagenes");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        enviarImagenesDeGuias(view, listRegistroImagen);
                    }
                }).start();

            }
        });

        builder.setNegativeButton(textoDelBotonNegativo, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
        AlertDialog dialog = builder.create();

        dialog.show();

    }

    private void enviarRegistroEntrega(final View view, final long xcodigoRegistro, final int qGuiasPorRegistrar) {

    }


    private void manejoDeMensajeGrabar(View view, int qGuiasPorRegistrar, String mensajeError) {

        if (qRegistrosEntregaEnviados == qGuiasPorRegistrar) { //se enviaron todas exitosamente
            mostrarMensajeExitoOErroreIrAListaEntregas(view, getResources().getString(R.string.msg_exito_title), getResources().getString(R.string.enviar_reg_entrega_msg_exito_contenido), getResources().getString(R.string.msg_exito_boton_aceptar_texto));

        } else {

            if (qRegistrosEntregaNoEnviados == qGuiasPorRegistrar) { //se enviaron todas sin exito
                mostrarMensajeExitoOErroreIrAListaEntregas(view, getResources().getString(R.string.msg_error_title), mensajeError, getResources().getString(R.string.msg_exito_boton_aceptar_texto));
            } else { //algunas se enviaron con exito otras no
                String mensajeExitoError = "Se enviaron " + qRegistrosEntregaEnviados + " guias correctamente, se enviaron a pendientes " + qRegistrosEntregaNoEnviados + " guias";
                mostrarMensajeExitoOErroreIrAListaEntregas(view, getResources().getString(R.string.msg_exito_error_title), mensajeExitoError, getResources().getString(R.string.msg_exito_boton_aceptar_texto));
            }
        }


    }

    private void enviarImagenesDeGuias(View view, Collection<RegistroRequest> listRegistroImagen) {

        for (RegistroRequest registroImagen : listRegistroImagen) {

            //Collection<Documento> listDocumento = registroImagen.getLista_documento();

            //for (Documento documento : listDocumento) {

            String solicitudAno = registroImagen.getSolicitudAno();
            int codigoRegistro = registroImagen.getCodigoRegistro();
            int solicitudNro = registroImagen.getSolicitudNro();
            String solicitudFechaRegistroMovil = registroImagen.getRegistroFecha();

            //String nroGuia = documento.getDocReferencial();
            String ruta = registroImagen.getUbicacion();
            Log.i(TAG, "enviarImagenesDeGuias: "  + ruta);
            File fFile = new File(ruta);

            try {
                if (existeImagenByRutaFile(fFile) == 1) {
                    //Log.i(TAG, "run: " + "duerme imagen por 5 sec " ) ;
                    //Thread.currentThread().sleep(5000);
                    semaphoreImagenes.acquire();
                    //Log.i(TAG, "enviarImagenesDeGuias: liberado para subir imagen " + nroGuia + ".jpg");
                    int codigoEmpresa=getCodigoEmpresa(view.getContext());
                    uploadImage(view, fFile, codigoRegistro, solicitudAno, solicitudNro, solicitudFechaRegistroMovil,codigoEmpresa);
                    //Thread.currentThread().sleep(5000);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //}
        }
    }

    private void uploadImage(final View view, final File file, final int mCodigoRegistro,
                             final String mSolicitudAno, final int mSolicitudNro, final String mFechaRegistro, int mcodigoEmpresa) {

        final String TAG = "UploadImage";


        //Create Upload Server Client
        RegistroRestService registroRestService = ApiClient.getClient().create(RegistroRestService.class);
        //File creating from selected URL
        //File file = new File(imagePath);

        // create RequestBody instance from file
        //RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        RequestBody requestFile = RequestBody.create(MediaType.parse("image"), file);

        // MultipartBody.Part is used to send also the actual file name
        MultipartBody.Part body = MultipartBody.Part.createFormData("uploaded_file", file.getName(), requestFile);

        // add another part within the multipart request
        String descriptionString = "codigo_registro=" + mCodigoRegistro + "*" +
                "solicitud_ano=" + mSolicitudAno + "*" +
                "solicitud_nro=" + mSolicitudNro + "*" +
                "fecha_registro_movil=" + mFechaRegistro + "*" +
                "codigoEmpresa=" + mcodigoEmpresa + "*" ;

        RequestBody description = RequestBody.create(MediaType.parse("multipart/form-data"), descriptionString);

        Call<HashMap<String, RegistroResponse>> resultCall = registroRestService.uploadImage(description, body);
        //Call<HashMap<String, RegistroResponse>> resultCall = registroRestService.uploadImage(body);

        // finally, execute the request
        resultCall.enqueue(new Callback<HashMap<String, RegistroResponse>>() {
            @Override
            public void onResponse(Call<HashMap<String, RegistroResponse>> call, Response<HashMap<String, RegistroResponse>> response) {

                HashMap<String, RegistroResponse> registroResponseBody = response.body();
                // Response Success or Fail
                if (response.message().equals("OK")) {

                    RegistroResponse resultadoRespuesta = registroResponseBody.get("PostImageResult");
                    if (resultadoRespuesta.getRespuesta().equals("1")) {
                        //RegistroBLO registroBLO = new RegistroBLO(view.getContext());
                        //int rpta = registroBLO.eliminarRegistroImagen(resultadoRespuesta.getCodigoRegistro());
                        int rpta =1;
                        if (rpta == 1) {
                            try {
                                eliminarImagen(file);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.i(TAG, "onResponse: " + " se envio la foto correctamente" + " respuesta: " + file.getName());

                    } else {

                        Log.i(TAG, "onResponse: " + " no se envio la foto " + file.getName());
                    }

                } else {

                    int rpta = enviarRegistroImagenAPendientes(mCodigoRegistro, view);
                    Log.i(TAG, "onResponse: " + " no se envio la foto " + file.getName());
                }
                semaphoreImagenes.release();
                Log.i(TAG, "onResponse: " + " release para upload de siguiente imagen ");
            }

            @Override
            public void onFailure(Call<HashMap<String, RegistroResponse>> call, Throwable t) {
                //progressDialog.dismiss();
                Log.i(TAG, "onFailure: " + " release para upload de siguiente imagen ");
                Log.i(TAG, "onFailure: " + " no se envio la foto" + file.getName());
                int rpta = enviarRegistroImagenAPendientes(mCodigoRegistro, view);
                semaphoreImagenes.release();
            }
        });
    }

    public static int eliminarImagen(File file) throws IOException {
        int rpta = 0;
        if (file.exists()) {
            if (file.delete()) {
                rpta = 1;
                Log.i(TAG, "eliminarImagenDeCarpetaTemporal: " + " se elimino");
            } else {
                rpta = 0;
                Log.i(TAG, "eliminarImagenDeCarpetaTemporal: " + " no se elimino");
            }
        }
        return rpta;
    }

    private int enviarRegistroImagenAPendientes(long xcodigoRegistro, View view) {


        int rpta_elimina = 1;

        return rpta_elimina;
    }

    private int eliminarDatosEnTablasAsociadosAlUsuarioySharedPreferences() {
        return 1;
    }

    private void mostrarMensajeExitoOErroreIrAListaEntregas(View view, String tituloMensaje, String contenidoMensaje, String textoDelBoton) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyAlertDialogTheme);
        builder.setMessage(contenidoMensaje)
                .setTitle(tituloMensaje);

        builder.setPositiveButton(textoDelBoton, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent intent = new Intent(RegistroEntrega.this, MainActivity.class);
                startActivity(intent);
                finish();

            }
        });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();

        dialog.show();

    }

    private Collection<RegistroRequest> getListaRegistroImagenPorEnviar(View view) {



        Collection<RegistroRequest> listRegistroImagen;
        Collection<RegistroRequest> listRegistroImagenTotal = new ArrayList<>();

        for (int i = 0; i < listaCodigoRegistrosImagenesInsertadas.size(); i++) {

            /*listRegistroImagen = registroRequestBLO.listar(listaCodigoRegistrosImagenesInsertadas.get(i).get("codigoRegistro"));

            for (RegistroRequest mRegistroImagen : listRegistroImagen) {
                listRegistroImagenTotal.add(mRegistroImagen);
                Log.i(TAG, "c: " + mRegistroImagen.getUbicacion());
            }*/
        }

        return listRegistroImagenTotal;
    }

    /*private Collection<RegistroRequest> getListaRegistroImagenPorEnviar(Context context) {

        *//*RegistroRequestBLO registroRequestBLO = new RegistroRequestBLO(context);

        Collection<RegistroRequest> listRegistroImagen;
        Collection<RegistroRequest> listRegistroImagenTotal = new ArrayList<>();

        Log.i(TAG, "getListaRegistroImagenPorEnviar: cantidad de registros " + Integer.toString(listaCodigoRegistrosImagenesInsertadas.size()));

        for (int i = 0; i < listaCodigoRegistrosImagenesInsertadas.size(); i++) {

            Log.i(TAG, Integer.toString(i) + " getListaRegistroImagenPorEnviar: codigoRegistro " + listaCodigoRegistrosImagenesInsertadas.get(i).get("codigoRegistro"));
            listRegistroImagen = registroRequestBLO.listar(listaCodigoRegistrosImagenesInsertadas.get(i).get("codigoRegistro"));

            for (RegistroRequest mRegistroImagen : listRegistroImagen) {
                listRegistroImagenTotal.add(mRegistroImagen);
                Log.i(TAG, "getListaRegistroImagenPorEnviar: " + mRegistroImagen.getUbicacion());
            }
        }

        return listRegistroImagenTotal;*//*
    }*/

    public int getCodigoEmpresa(Context contexto){

        return 1;
    }

    private int existeElementoRepetidoListaGuia(ArrayList<HashMap<String, String>> lista, String valorVerificar ){

        int i=0;
        int existeValor=0;

        while (i<lista.size() && existeValor==0)  {

            HashMap<String, String> mapa =lista.get(i);
            Log.i("arrat", "verificaElementoRepetidoListaGuia: "  + mapa.toString());
            if (mapa.containsValue(valorVerificar)){
                existeValor=1;
            };
            i++;
        }

        return existeValor;
    }

    private void actualizaIconosCamaraEnListaGuias(ArrayList<HashMap<String, String>> lista ){

        int i=0;
        while (i<lista.size())  {
            //Log.i(TAG, "actualizaIconosCamaraEnListaGuias: entra al while");

            HashMap<String, String> mapa =lista.get(i);;

            String mNroGuia=mapa.get("nro_guia");
            int mTomaFotoCantidad=Integer.parseInt(mapa.get("toma_foto_cantidad"));
            int guiaCantidadFotosExistentes=getCantidadFotosExistentesByGuia(mNroGuia,mTomaFotoCantidad);
            int guiaCantidadAdjuntosExistentes=getCantidadAdjuntosExistentesByGuia(mNroGuia);
            //Log.i(TAG, "cantidad de fotos existentes: " + guiaCantidadFotosExistentes);
            //Log.i(TAG, "cantidad de fotos por tomar: " + mTomaFotoCantidad);


            if (guiaCantidadFotosExistentes<mTomaFotoCantidad){//hay pendientes
                mapa.put("opc_adjuntar_imagen", Integer.toString(R.drawable.ic_attach_file));
                mapa.put("opc_registrar_imagen", Integer.toString(R.drawable.ic_agregar_foto));
                if (guiaCantidadFotosExistentes>0 || guiaCantidadAdjuntosExistentes>0){//si hay alguna foto o adjunto
                    mapa.put("opc_vista_previa", Integer.toString(R.drawable.ic_preview_photo));
                }else{
                    mapa.put("opc_vista_previa", Integer.toString(R.drawable.preview_transparente));
                }
            }else{
                mapa.put("opc_adjuntar_imagen", Integer.toString(R.drawable.ic_attach_file));
                mapa.put("opc_registrar_imagen", Integer.toString(R.drawable.ic_camera));
                mapa.put("opc_vista_previa", Integer.toString(R.drawable.ic_preview_photo));
            }
            listGuiasAdapter.notifyDataSetChanged();

            i++;
        }
    }

    public int getCantidadFotosExistentesByGuia(String mNroGuia, int mTomaFotoCantidad){

        int tomaFotoCantidadExistente=0;

        //verifica cuantas imagenes tiene la guia
        if (mTomaFotoCantidad>1){
            String rutaCarpetaImagenes=getExternalFilesDir(null).getParent().toString() + "/" + nombreCarpetaImagenes  ;

            File carpetaImagenes = new File(rutaCarpetaImagenes);

            File[] files = carpetaImagenes.listFiles();

            if (files!=null){

                String filename = "";
                for (File file : files) {

                    filename = file.getName();
                    int positionCaracterFoto=filename.indexOf("__");
                    int positionCaracterAdjunto=filename.indexOf("__ADJ");// el archivo de la foto adjunta tiene este formato
                    if (positionCaracterFoto>0 && positionCaracterAdjunto==-1){
                        String nroGuiaExtraida= filename.substring(0,positionCaracterFoto);
                        if (mNroGuia.equals(nroGuiaExtraida)){
                            tomaFotoCantidadExistente++;
                        }
                    }
                }
            }
        }else{ //cuando es 1
            String ruta=getExternalFilesDir(null).getParent().toString() + "/" + nombreCarpetaImagenes + "/" + mNroGuia  + ".jpg";
            File fFile=new File(ruta);
            try {
                if (existeImagenByRutaFile(fFile)==1){
                    tomaFotoCantidadExistente++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return tomaFotoCantidadExistente;

    }
    public int getCantidadAdjuntosExistentesByGuia(String mNroGuia){

        int adjuntoCantidadExistente=0;

        //verifica cuantas imagenes tiene la guia
        String rutaCarpetaImagenes=getExternalFilesDir(null).getParent().toString() + "/" + nombreCarpetaImagenes  ;

        File carpetaImagenes = new File(rutaCarpetaImagenes);

        File[] files = carpetaImagenes.listFiles();

        if (files!=null){

            String filename = "";
            for (File file : files) {

                filename = file.getName();
                int positionCaracterAdjunto=filename.indexOf("__ADJ");// el archivo de la foto adjunta tiene este formato
                if (positionCaracterAdjunto>0){
                    String nroGuiaExtraida= filename.substring(0,positionCaracterAdjunto);
                    if (mNroGuia.equals(nroGuiaExtraida)){
                        adjuntoCantidadExistente++;
                    }
                }
            }
        }

        return adjuntoCantidadExistente;

    }
    public String getNombresFotosExistentesByGuia(String mNroGuia, int mTomaFotoCantidad){

        String nombreFotosExistentes="";

        //verifica cuantas imagenes tiene la guia
        if (mTomaFotoCantidad>1){
            String rutaCarpetaImagenes=getExternalFilesDir(null).getParent().toString() + "/" + nombreCarpetaImagenes  ;

            File carpetaImagenes = new File(rutaCarpetaImagenes);

            File[] files = carpetaImagenes.listFiles();

            if (files!=null){

                String filename = "";
                for (File file : files) {

                    filename = file.getName();
                    int positionCaracterFoto=filename.indexOf("__");
                    if (positionCaracterFoto>0){
                        String nroGuiaExtraida= filename.substring(0,positionCaracterFoto);
                        if (mNroGuia.equals(nroGuiaExtraida)){
                            nombreFotosExistentes=filename;
                        }
                    }
                }
            }
        }else{ //cuando es 1
            String ruta=getExternalFilesDir(null).getParent().toString() + "/" + nombreCarpetaImagenes + "/" + mNroGuia  + ".jpg";
            File fFile=new File(ruta);
            try {
                if (existeImagenByRutaFile(fFile)==1){
                    nombreFotosExistentes=fFile.getName();
                }else{//sino encuentra foto que busque imagen adjunta
                    nombreFotosExistentes=getNombreImagenAdjuntaExistenteByGuia(mNroGuia);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return nombreFotosExistentes;

    }
    public String getNombreImagenAdjuntaExistenteByGuia(String mNroGuia){

        String nombreImagenAdjunta="";

        //verifica cuantas imagenes tiene la guia
        String rutaCarpetaImagenes=getExternalFilesDir(null).getParent().toString() + "/" + nombreCarpetaImagenes  ;

        File carpetaImagenes = new File(rutaCarpetaImagenes);

        File[] files = carpetaImagenes.listFiles();

        if (files!=null){

            String filename = "";
            for (File file : files) {

                filename = file.getName();
                int positionCaracterFoto=filename.indexOf("__ADJ"); //formato de nombre de imagen adjunta
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
    private void calcularCantidadGuias(){

        String  cantidad= Integer.toString(listGuias.size());
        TextView txtvGuia=(TextView)findViewById(R.id.reg_entrega_cantidad_guias);
        txtvGuia.setText(cantidad);
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

    /*public void llenarListaCombos() {

        //combo Tipo Tabla
        TablaBLO tablaBLO = new TablaBLO(this.getApplicationContext());

        ArrayList<Tabla> listTipoEntrega = new ArrayList<>(tablaBLO.listarCodigoDescripcion("0015")); //0015 codigo de tipo tabla tipo_entregas
        String codigoTipoEntregaPredeterminado=tablaBLO.getCodigoTablaPredeterminadoByTipoTabla("0015");

        Spinner spinnerTipoEntrega = (Spinner) findViewById(R.id.reg_entrega_spinner_tipo);
        ArrayAdapter<Tabla> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, listTipoEntrega );
        dataAdapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        spinnerTipoEntrega.setAdapter(dataAdapter);
        //setear valor predeterminado
        int posicionTipoEntregaPredeterminado=getPosicionEnSpinnerByCodigoCampo(spinnerTipoEntrega,codigoTipoEntregaPredeterminado);
        spinnerTipoEntrega.setSelection(posicionTipoEntregaPredeterminado);

        //combo Lugar de Entrega
        ArrayList<Tabla> listLugarEntrega = new ArrayList<>(tablaBLO.listarCodigoDescripcion("0005")); //0005 codigo de tipo tabla lugar_entregas
        String codigoLugarEntregaPredeterminado=tablaBLO.getCodigoTablaPredeterminadoByTipoTabla("0005");

        Spinner spinnerLugarEntrega = (Spinner) findViewById(R.id.reg_entrega_spinner_lugar);
        ArrayAdapter<Tabla> dataAdapterLugarEntrega = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, listLugarEntrega );
        dataAdapterLugarEntrega.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        spinnerLugarEntrega.setAdapter(dataAdapterLugarEntrega);

        int posicionLugarEntregaPredeterminado=getPosicionEnSpinnerByCodigoCampo(spinnerLugarEntrega,codigoLugarEntregaPredeterminado);
        spinnerLugarEntrega.setSelection(posicionLugarEntregaPredeterminado);

       *//* Spinner spinnerLugarEntrega2 = (Spinner) findViewById(R.id.reg_entrega_spinner_lugar2);
        ArrayAdapter<Tabla> dataAdapterLugarEntrega2 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, listTipoEntrega);
        dataAdapterLugarEntrega2.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        spinnerLugarEntrega2.setAdapter(dataAdapterLugarEntrega2);*//*

    }*/

    public int getPosicionEnSpinnerByCodigoCampo(Spinner spinner, String codigoCampo){

        int i=0 ;
        int posicion=-1 ;
        boolean find=false;

        while (i<spinner.getCount() && find==false){
        i++;
        }
        return posicion;
    }

    private void scanearCodigoBarra(){
        //imgBtn = (ImageButton)findViewById(R.id.reg_entrega_opc_button_escanear);
        contentTxt = (TextView)findViewById(R.id.reg_entrega_etxt_guia);


    }

    private void mostrarMensaje(View view,String tituloMensaje, String contenidoMensaje, String textoDelBoton){

        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setMessage(contenidoMensaje)
                .setTitle(tituloMensaje);

        builder.setPositiveButton(textoDelBoton, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
        AlertDialog dialog = builder.create();

        dialog.show();

    }

    private void mostrarMensaje(Context context, String tituloMensaje, String contenidoMensaje, String textoDelBoton){

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(contenidoMensaje)
                .setTitle(tituloMensaje);

        builder.setPositiveButton(textoDelBoton, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
        AlertDialog dialog = builder.create();

        dialog.show();

    }

    public void ocultarSoftKeyboard(){
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private  void agregarListaGuiasSeleccionadas(ArrayList<HashMap<String, String>> mListaGuias){

        int i =0;
        int cantidad=mListaGuias.size();
        while (i<cantidad){
            HashMap<String, String> mapa =mListaGuias.get(i);
            int solicitud_nro=Integer.parseInt(mapa.get("solicitud_nro"));
            agregaDatosDeGuiaEnLista (mapa.get("solicitud_ano").toString(), solicitud_nro ,mapa.get("nro_guia").toString(),
                    mapa.get("flg_registrar_imagen").toString() , Integer.parseInt(mapa.get("toma_foto_cantidad").toString()));
            i++;
        }
    }

    private String getCadenaDeGuiasAExcluir(ArrayList<HashMap<String, String>> listaGuias){

        int i=0;
        ArrayList<String> arrayGuias = new ArrayList<>();

        while (i<listaGuias.size() )  {

            HashMap<String, String> mapa =listaGuias.get(i);

            arrayGuias.add("'" + mapa.get("nro_guia") + "'" );
            i++;

        }
        String stringGuias = arrayGuias.toString();

        stringGuias = stringGuias.replace("[","(");
        stringGuias = stringGuias.replace("]",")");

        return stringGuias ;
    }

    private String getGuiasSinFoto(ArrayList<HashMap<String, String>> listaGuias){

        int i=0;
        String cadenaGuias="";

        while (i<listaGuias.size() )  {

            HashMap<String, String> mapa =listaGuias.get(i);
            String nroGuia=mapa.get("nro_guia").toString();
            String flgRegistrarImagen=mapa.get("flg_registrar_imagen").toString();
            int mTomaFotoCantidad=Integer.parseInt(mapa.get("toma_foto_cantidad"));
            int guiaCantidadFotosExistentes=getCantidadFotosExistentesByGuia(nroGuia,mTomaFotoCantidad);

            if (flgRegistrarImagen.equals("1")){ //si exige imagen
                if (guiaCantidadFotosExistentes<mTomaFotoCantidad){//hay pendientes
                    cadenaGuias=cadenaGuias + nroGuia + "\n";
                }
            }
            i++;

        }
        if (cadenaGuias.length()>1){
            cadenaGuias=cadenaGuias.substring(0,cadenaGuias.length()-1);
        }
        return cadenaGuias ;
    }

    private ArrayList<HashMap<String, String>> getListaGuiasConFoto(ArrayList<HashMap<String, String>> listaGuias){

        int i=0;
        ArrayList<HashMap<String, String>> listaGuiasConFoto= new ArrayList();

        while (i<listaGuias.size() )  {

            HashMap<String, String> mapa =listaGuias.get(i);
            String nroGuia=mapa.get("nro_guia").toString();
            int tomaFotoCantidad=Integer.parseInt(mapa.get("toma_foto_cantidad").toString());
            String rutaCarpetaImagenes=getExternalFilesDir(null).getParent().toString() + "/" + nombreCarpetaImagenes  ;

            if (tomaFotoCantidad>1){//cuando hay varias fotos por guia

                File carpetaImagenes = new File(rutaCarpetaImagenes);
                File[] files = carpetaImagenes.listFiles();
                if (files!=null){
                    String fileName="";
                    for (File file : files) {
                        fileName = file.getName();
                        int positionCaracterFoto=fileName.indexOf("__");
                        if (positionCaracterFoto>0){
                            String nroGuiaExtraida= fileName.substring(0,positionCaracterFoto);
                            if (nroGuia.equals(nroGuiaExtraida)){
                                HashMap<String, String> guiaConFoto = new HashMap<String, String>();
                                guiaConFoto.put("solicitud_ano", mapa.get("solicitud_ano").toString());
                                guiaConFoto.put("solicitud_nro", mapa.get("solicitud_nro").toString());
                                guiaConFoto.put("nro_guia", mapa.get("nro_guia").toString());
                                guiaConFoto.put("ubicacion", rutaCarpetaImagenes + "/" + fileName );

                                listaGuiasConFoto.add(guiaConFoto);
                            }
                        }
                    }
                }
            } else {
                String ruta = getExternalFilesDir(null).getParent().toString() + "/" + nombreCarpetaImagenes + "/" + nroGuia + ".jpg";
                File fFile = new File(ruta);

                try {
                    //buscar la imagen tomada por camara
                    HashMap<String, String> guiaConFoto = new HashMap<String, String>();
                    if (existeImagenByRutaFile(fFile) == 1) { //existe imagen de foto
                        guiaConFoto.put("solicitud_ano", mapa.get("solicitud_ano").toString());
                        guiaConFoto.put("solicitud_nro", mapa.get("solicitud_nro").toString());
                        guiaConFoto.put("nro_guia", mapa.get("nro_guia").toString());
                        guiaConFoto.put("ubicacion", ruta);
                        listaGuiasConFoto.add(guiaConFoto);
                    }
                    //busca imagen adjunta
                    HashMap<String, String> guiaConAdjunto = new HashMap<String, String>();
                    ruta=getNombreImagenAdjuntaExistenteByGuia(nroGuia);
                    if (!ruta.equals("")){
                        guiaConAdjunto.put("solicitud_ano", mapa.get("solicitud_ano").toString());
                        guiaConAdjunto.put("solicitud_nro", mapa.get("solicitud_nro").toString());
                        guiaConAdjunto.put("nro_guia", mapa.get("nro_guia").toString());
                        guiaConAdjunto.put("ubicacion", rutaCarpetaImagenes + "/" + ruta);
                        listaGuiasConFoto.add(guiaConAdjunto);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            i++;

        }
        listarArrayHashMap(listaGuiasConFoto);
        return listaGuiasConFoto ;
    }

    private  void listarArrayHashMap(ArrayList<HashMap<String, String>> mArrayHashMap){

        int i =0;
        int cantidad=mArrayHashMap.size();
        Log.i(TAG, "cantidad de registros por Agregar " + cantidad);
        while (i<cantidad){
            HashMap<String, String> mapa =mArrayHashMap.get(i);
            Log.i(TAG, "" + cantidad + " guia:" + mapa.get("nro_guia").toString()
                    + " ----> ano :"       + mapa.get("solicitud_ano").toString()
                    + " ----> nro :"       + mapa.get("solicitud_nro").toString()
                    + " ----> ubicacion :" + mapa.get("ubicacion").toString());
            i++;
        }
        Log.i(TAG, "seleccionados: " + mArrayHashMap.size());
    }

    //LOCATION
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");

        // If the initial location was never previously requested, we use
        // FusedLocationApi.getLastLocation() to get it. If it was previously requested, we store
        // its value in the Bundle and check for it in onCreate(). We
        // do not request it again unless the user specifically requests location updates by pressing
        // the Start Updates button.
        //
        // Because we cache the value of the initial location in the Bundle, it means that if the
        // user launches the activity,
        // moves to a new location, and then changes the device orientation, the original location
        // is displayed as the activity is re-created.

        if (mCurrentLocation == null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

//            Log.i(TAG, "onConnected: " + Double.toString(mCurrentLocation.getLatitude())  + "  accuracy: " + mCurrentLocation.getAccuracy() + " time: " + mCurrentLocation.getTime() );
//            Log.i(TAG, "onConnected: " + Double.toString(mCurrentLocation.getLongitude()) + "  accuracy: " + mCurrentLocation.getAccuracy() + " time: " + mCurrentLocation.getTime());
        }

        // If the user presses the Start Updates button before GoogleApiClient connects, we set
        // mRequestingLocationUpdates to true (see startUpdatesButtonHandler()). Here, we check
        // the value of mRequestingLocationUpdates and if it is true, we start location updates.
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    /**
     * Callback that fires when the location changes.
     */
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        mMyLocationLatLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

        //Log.i(TAG, "onLocationChanged: " + Double.toString(mCurrentLocation.getLatitude())  + "  accuracy: " + mCurrentLocation.getAccuracy() + " time: " + mCurrentLocation.getTime() );
        //Log.i(TAG, "onLocationChanged: " + Double.toString(mCurrentLocation.getLongitude()) + "  accuracy: " + mCurrentLocation.getAccuracy() + " time: " + mCurrentLocation.getTime());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }
    //FIN LOCATION
}
