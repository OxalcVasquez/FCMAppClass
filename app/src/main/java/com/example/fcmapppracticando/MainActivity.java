package com.example.fcmapppracticando;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fcmapppracticando.config.Config;
import com.example.fcmapppracticando.utils.NotificationUtils;
import com.example.modelo.Preferencia;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;

import java.util.zip.Inflater;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    BroadcastReceiver broadcastReceiver;
    TextView txtTituloMensajeRemoto,txtMensajeRemoto,txtTemaDeInteres;
    Button btnSuscriete,btnAnularSubscripcion,btnLogin;
    //Firebase Authentication
    FirebaseAuth firebaseAuth;
    String authId = "";
    //Firebase Database Realtime
    FirebaseDatabase db;
    DatabaseReference nodoPreferencias;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtTituloMensajeRemoto = findViewById(R.id.txtTituloMensajeRemoto);
        txtMensajeRemoto = findViewById(R.id.txtTextoMensajeRemoto);
        txtTemaDeInteres = findViewById(R.id.txtTemaInteres);
        btnSuscriete = findViewById(R.id.btnSuscribete);
        btnAnularSubscripcion = findViewById(R.id.btnAnularSuscripcion);
        btnLogin = findViewById(R.id.btnLogin);

        btnSuscriete.setOnClickListener(this);
        btnAnularSubscripcion.setOnClickListener(this);
        btnLogin.setOnClickListener(this);

        /* Firebase Auth */
        firebaseAuth = FirebaseAuth.getInstance();
        iniciarSesion();
        /* Firebase Auth */

        /* Firebase Realtime Database */
        db = FirebaseDatabase.getInstance();
        nodoPreferencias = db.getReference("preferencias");
        /* Firebase Realtime Database */

        /* Desahbilitar botones */
        btnSuscriete.setEnabled(false);
        btnAnularSubscripcion.setEnabled(false);
        btnLogin.setVisibility(View.INVISIBLE);


        //Configuar el receptor broadcast Receiver
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Cuando Instalao la aplicacion
                if (intent.getAction().equals(Config.REGISTRATION_COMPLETE)){
                    //Abrir un canal de comunicaicón GLOBAL, le llegaran todas las notificaciones al usuario
                    FirebaseMessaging.getInstance().subscribeToTopic(Config.TOPIC_GLOBAL);
                } else if (intent.getAction().equals(Config.PUSH_NOTIFICATION)){
                    //Cuando se recibe la notificacion
                    String title = intent.getExtras().getString("firebase_title");
                    String message = intent.getExtras().getString("firebase_message");
                    String timestamp = intent.getExtras().getString("firebase_timestamp");

                    Log.e("title__",title);
                    Log.e("title__mensaje",message);


                    //Mostrar los datos del mensaje en el activity
                    txtTituloMensajeRemoto.setText(title);
                    txtMensajeRemoto.setText(message);

                    MainActivity.this.setTitle("Fecha y hora : "+timestamp);

                }
            }
        };

        //Registrar el MainActivity como receptor de los mensajes remotos
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,new IntentFilter(Config.REGISTRATION_COMPLETE));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,new IntentFilter(Config.PUSH_NOTIFICATION));

        if (this.getIntent().getExtras() !=  null){
            String title = this.getIntent().getExtras().getString("firebase_title");
            String message = this.getIntent().getExtras().getString("firebase_message");
            String timestamp = this.getIntent().getExtras().getString("firebase_timestamp");
            Log.e("title",title);
            Log.e("message",message);

            //Mostrar los datos del mensaje en el activity
            txtTituloMensajeRemoto.setText(title);
            txtMensajeRemoto.setText(message);
            MainActivity.this.setTitle("Fecha y hora : "+timestamp);
        }


    }




    @Override
    public void onClick(View view) {
        if (txtTemaDeInteres.getText().toString().isEmpty() && !authId.isEmpty()){
            Toast.makeText(this, "Ingrese tema de interes", Toast.LENGTH_SHORT).show();
            return;
        }
        //Capturar el tema de interes ingresado por el usuario
        String tema = NotificationUtils.eliminarAcentos(txtTemaDeInteres.getText().toString());
        switch (view.getId()){
            case R.id.btnSuscribete:
                FirebaseMessaging.getInstance().subscribeToTopic(tema);
                registrarPreferencias(tema);
                Toast.makeText(this, "Se ha registrado la subscripcion", Toast.LENGTH_SHORT).show();
                break;

            case R.id.btnAnularSuscripcion:
                FirebaseMessaging.getInstance().unsubscribeFromTopic(tema);
                eliminarPreferencias(tema);
                Toast.makeText(this, "Se ha anulado la subscripcion", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btnLogin:
                iniciarSesion();
                break;
        }

        txtTemaDeInteres.setText("");
    }

    private void iniciarSesion() {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View prompt = layoutInflater.inflate(R.layout.login_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(prompt);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setTitle("Iniciar sesión Firebase");

        //Declarar los controles del dialog
        TextInputEditText txtEmailLogin,txtContrasenaLogin;
        Button btnIniciarSesionDialog,btnSalirDialog;
        ProgressBar progressBar;
        //Enlazar los controles
        txtEmailLogin = prompt.findViewById(R.id.txtEmailLogin);
        txtContrasenaLogin = prompt.findViewById(R.id.txtContrasenaLogin);
        btnIniciarSesionDialog = prompt.findViewById(R.id.btnIniciarSesion);
        btnSalirDialog = prompt.findViewById(R.id.btnSalir);
        progressBar = prompt.findViewById(R.id.progressBar);

        txtEmailLogin.requestFocus();

        AlertDialog alertDialog = alertDialogBuilder.show();

        //Implementar la funciondalidad de los botones
        btnIniciarSesionDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /* Validacion de los datos */
                progressBar.setVisibility(View.VISIBLE);
                firebaseAuth.signInWithEmailAndPassword(txtEmailLogin.getText().toString(),txtContrasenaLogin.getText().toString()).addOnCompleteListener(
                        MainActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()){
                                    FirebaseUser user = firebaseAuth.getCurrentUser();
                                    authId = user.getUid();
                                    Log.e("AUTH ID",authId);
                                    btnSuscriete.setEnabled(true);
                                    btnAnularSubscripcion.setEnabled(true);
                                    btnLogin.setVisibility(View.INVISIBLE);

                                    alertDialog.dismiss();
                                } else {
                                    Toast.makeText(MainActivity.this, "Credenciales Incorrectas", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                );
            }
        });

        btnSalirDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
                btnLogin.setVisibility(View.VISIBLE);
            }
        });

        alertDialog.show();

    }

    private void registrarPreferencias(String nombrePreferencia) {
        //Generar Id de la preferencia
        String id = nodoPreferencias.push().getKey();

        //Instanciar a la clase preferencia para setear los datos
        Preferencia preferencia = new Preferencia(id,nombrePreferencia);

        //Registrar la preferencia en la BD Realtime Database
        nodoPreferencias.child(authId).child(id).setValue(preferencia);
    }

    private void eliminarPreferencias(String nombrePreferencia) {
        nodoPreferencias.child(authId).orderByChild("nombre").equalTo(nombrePreferencia).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for ( DataSnapshot child: snapshot.getChildren()) {
                    Toast.makeText(MainActivity.this, child.getValue().toString(), Toast.LENGTH_SHORT).show();
                    Log.e("Preferencia value", child.getValue().toString());
                    child.getRef().removeValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }




}