package com.sandra.falldetector2.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sandra.falldetector2.util.MqttManagerAndroid;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.android.schedulers.AndroidSchedulers;
import com.sandra.falldetector2.App;
import com.sandra.falldetector2.R;
import com.sandra.falldetector2.adapter.ContactAdpter;
import com.sandra.falldetector2.model.Contact;
import com.sandra.falldetector2.repository.ContactRepository;
import com.sandra.falldetector2.util.SmsDeliveredReceiver;
import com.sandra.falldetector2.util.SmsSentReceiver;

public class RegisterActivity extends AppCompatActivity implements MessageClient.OnMessageReceivedListener {

    public static final String SMS_DELIVERED = "SMS_DELIVERED";
    public static final String SMS_SENT = "SMS_SENT";
    private BroadcastReceiver sentStatusReceiver;
    private BroadcastReceiver deliveredStatusReceiver;

    private static final String
            FALL_CAPABILITY_NAME = "fall_notification";

    @BindView(R.id.toolbar_right_button)
    Button rigthButton;
    @BindView(R.id.toolbar_title)
    TextView toolbarTitle;
    @BindView(R.id.toolbar_left_button)
    ImageButton leftButton;
    private Toolbar toolbar;
    private ContactRepository contactRepository;
    @BindView(R.id.contactList)
    RecyclerView contactList;
    MqttManagerAndroid mqttManagerAndroid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        contactRepository = App.getInstance().getContactRepository();
        FloatingActionButton fab = findViewById(R.id.fab);
        mqttManagerAndroid = new MqttManagerAndroid(this);

        //Evento de clique do FAB para cadastrar novo email de emergência.
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showNewEmailDialog();
            }
        });

        //Adicionar listener nessa acitivity para receber mensagens do watch
        Wearable.getMessageClient(this).addListener(this);

        configRecyclerView();

        setContactListAdpter();

        //Ao iniciar a activity já as permissões de usuário em tempo de execucação
        new RxPermissions(this)
                .request(Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.SEND_SMS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isGranted -> {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {

                    }


                }, error -> {
                });


    }

    //Méto para enviar uma SMS para todos o usuários do banco de dados
    private void sendSmsToAll(){

        String username = PreferenceManager.getDefaultSharedPreferences(this).getString("username", null);
        //Obtém a localização do usuário
        Location location = App.getInstance().getLocation();
        String message = "";
        if (location != null)
            message = "Queda de" + username + "em: https://www.google.com/maps/search/?api=1&query="+ location.getLatitude()+","+location.getLongitude();
        else
            message = "Alerta de" + username + ", que provavelmente sofreu uma queda.";
        Contact[] contacts = contactRepository.getAllContacts();
        if (contacts.length > 0){
            //Percorre todos os contatos e envia a SMS
            for (Contact c:contacts){
                if (c.isImportant()) {
                    String number = c.getNumber().replace("(", "").replace(")", "").replace("-", "").replace(" ", "");
                    String phone = "+55" + number;
                    sendSMS(phone, message);
                    Log.d("teste", "sendSmsToAll: " + phone);
                }
            }
        }
    }

    //Método para enviar a SMS utilizando os recursos do SO Android
    private void sendSMS(String phoneNumber, String message) {
        SmsManager sms = SmsManager.getDefault();
        // if message length is too long, messages are divided
        List<String> messages = sms.divideMessage(message);
        for (String msg : messages) {
            PendingIntent sentIntent = PendingIntent.getBroadcast(
                    this, 0, new Intent(SMS_SENT), 0);
            PendingIntent deliveredIntent = PendingIntent.getBroadcast(
                    this, 0, new Intent(SMS_DELIVERED), 0);
            sms.sendTextMessage(phoneNumber, null, msg, sentIntent, deliveredIntent);
        }
        this.registerBroadcastReceiverForSms();

    }

    public void registerBroadcastReceiverForSms() {
        this.sentStatusReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context arg0, Intent arg1) {

                switch (this.getResultCode()) {
                    case Activity.RESULT_OK:

                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:

                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:

                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:

                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:

                        break;
                    default:
                        break;
                }

                unregisterReceiver(this);
            }
        };
        this.deliveredStatusReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (this.getResultCode()) {
                    case Activity.RESULT_OK:

                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                }
            }
        };
        this.registerReceiver(this.sentStatusReceiver, new IntentFilter(SMS_SENT));
        this.registerReceiver(this.deliveredStatusReceiver, new IntentFilter(SMS_DELIVERED));
    }

    //Método para ligar para o contato selecionado
    public void callNumber(){
        Contact[] contacts = contactRepository.getAllContacts();
        for (Contact c: contacts){
            //Apenas realiza a ligação para o contato que for selecionado como importante
            if (c.isImportant()){
                String number = c.getNumber().replace("(","").replace(")","").replace("-","").replace(" ","");
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:+55" + number));
                startActivity(intent);
            }
        }

    }

    //Método para mostrar um alerta na tela
    void showToast(String text) {

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Atenção");
        builder.setMessage(text);
        builder.setPositiveButton("OK", null);
        android.app.AlertDialog toastDialog = builder.create();
        toastDialog.show();
    }


    //Método para realizar a configuração inicial da RecyclerView
    public void configRecyclerView() {

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        contactList.setLayoutManager(layoutManager);
        contactList.addItemDecoration(new DividerItemDecoration(this, LinearLayout.VERTICAL));
    }

    //Método para carregar o Adpter com os contatos
    public void setContactListAdpter(){

        Contact[] contacts = App.getInstance().getContactRepository().getAllContacts();
        if(contacts!=  null && contacts.length > 0){
            ContactAdpter adpter = new ContactAdpter(contacts);
            contactList.setAdapter(adpter);
            contactList.requestLayout();
        }

    }


    //Dialog para cadastrar um novo usuário no banco de dados local
    public void showNewEmailDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View view = RegisterActivity.this.getLayoutInflater().inflate(R.layout.dialog_new_contact, null);
        final EditText name = view.findViewById(R.id.name_text);
        final EditText mail = view.findViewById(R.id.mail_text);
        builder.setView(view);
        builder.setCancelable(false);
        AlertDialog alertDialog = builder.create();
        alertDialog.setCancelable(false);
        alertDialog.setOnShowListener(dialogInterface -> {

            Button positiveButton = view.findViewById(R.id.ok_button);
            positiveButton.setOnClickListener(v -> {
                //Após o dialogo aparecer na tela e o usuário clicar no botão de confirmar
                //Salva o contato no banco de dados
                saveContact(name.getText().toString(),mail.getText().toString());
                dialogInterface.dismiss();
            });

            Button negativeButton = view.findViewById(R.id.cancel_button);
            negativeButton.setOnClickListener(v -> dialogInterface.dismiss());

        });
        alertDialog.show();
    }


    //Método para salvar o contato no banco de dados
    public void saveContact(String name, String mail){
        Contact c = new Contact(name,mail,false);
        App.getInstance().getContactRepository().saveContact(c);
        setContactListAdpter();
    }


    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {

        //Verifica se a mensagem é do tipo fall_notification
        if (messageEvent.getPath().equals(FALL_CAPABILITY_NAME)) {
            showToast("Queda detectada!: " );
            sendSmsToAll();
            callNumber();
            String username = PreferenceManager.getDefaultSharedPreferences(this).getString("username", null);
            String topic = "/" + username.replace(" ","-");
            mqttManagerAndroid.publishMessage(username + " sofreu uma queda",topic);
        }
    }


}
