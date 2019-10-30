package com.sandra.falldetector2;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends WearableActivity implements SensorEventListener {

    private TextView mTextView;
    //Intevalo entre as leituras do acelerometro que equivale a cada 0,02 seg e frequencia 50Hz
    private static final int ACCELEROMETER_SAMPLING_PERIOD = 20000;
    //Limiar da soma das aceleraçoes
    private static final double CSV_THRESHOLD = 23;
    //Limiar da variacao do angulo
    private static final double CAV_THRESHOLD = 18;
    //Limiar da variacao do angulo
    private static final double CCA_THRESHOLD = 65.5;

    //Armazenamento dos valores do acelerometro
    private List<Map<AccelerometerAxis, Double>> accelerometerValues = new ArrayList<>();

    //Armazenanto dos valores para calcular o desvio padrao
    private List<Map<AccelerometerAxis, Double>> accelerometerValuesDesvPadrao = new ArrayList<>();

    //Armazenamento dos valores apos detectar a queda em 0.4 seg
    private List<Map<AccelerometerAxis, Double>> accelerometerValues04seg = new ArrayList<>();

    //Variavel para controlar os dados lidos do sensor
    private SensorManager sensorManager;
    //Variavel para controlar o cronometro
    CountDownTimer countDownTimer;

    private TextView mTextView2;
    private TextView mTextView3;
    private ImageButton imageButton;
    private static final String
            FALL_CAPABILITY_NAME = "fall_notification";

    //Variavel para armazenar que ira se comunicar com o watch
    private String transcriptionNodeId = null;

    //Variavel que controlar


    boolean countOneQuarterseg = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Obtem a referencias das vies utilizadas no layout
        mTextView = findViewById(R.id.textView);
        mTextView2 = findViewById(R.id.textView2);
        mTextView3 = findViewById(R.id.textView3);
        imageButton = findViewById(R.id.imageButton);
        //Metodo de click do botao para os casos de falso positivo.
        //Ao clicar no botao as leituras vao sem retomadas assim como o cronometro
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initSensor();
                mTextView.setVisibility(View.VISIBLE);
                mTextView.setText("Monitorando...");
                mTextView2.setVisibility(View.GONE);
                mTextView3.setVisibility(View.GONE);
                imageButton.setVisibility(View.GONE);
                countDownTimer.cancel();
                accelerometerValues = new ArrayList<>();
                accelerometerValues04seg = new ArrayList<>();
                accelerometerValuesDesvPadrao = new ArrayList<>();
                countDownTimer = null;
            }
        });
        //Iniciar a leitura dos dados do sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        initSensor();

        //Codigo que inicia a atividade do relogio e indica que ele pronto para se comunicar e já busca o novo node
        Wearable.getCapabilityClient(this)
                .getCapability(FALL_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE)
                .addOnCompleteListener(new OnCompleteListener<CapabilityInfo>() {
                    @Override
                    public void onComplete(@NonNull Task<CapabilityInfo> task) {
                        if (task.getResult() != null)
                            updateTranscriptionCapability(task.getResult());
                    }
                });

        setAmbientEnabled();
    }

    //Metodo que inicia a leitura do sensor
    public void initSensor() {

        if( sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() > 0) {

            sensorManager.registerListener(
                    this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    ACCELEROMETER_SAMPLING_PERIOD
            );
        }

    }

    //Metodo que interrompe a leitura do sensor
    public void stopReadings(){
        sensorManager.unregisterListener(this);
        Log.d("teste", "stopReadings: ");
    }


    @Override
    public final void onAccuracyChanged(Sensor sensor,
                                        int accuracy) {

    }

    //Este metodo vai ser chamada a cada nova leitura nova do sensor
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Axis of the rotation sample, not normalized yet.
        double x = event.values[0];
        double y = event.values[1];
        double z = event.values[2];

        if (this.isFallDetected(x, y, z)) {
            //Apos detectar a queda obtem as leituras nos proximos 0.4 seg no tempo
            countOneQuarterseg = true;
        }

    }


    //Metodo responsavel por alterar o layout para indicar uma possivel queda
    public void setupFallLayout() {
        startCronometer();
        mTextView.setText("Queda detectada!");
        mTextView.setVisibility(View.GONE);
        mTextView2.setVisibility(View.VISIBLE);
        mTextView3.setVisibility(View.VISIBLE);
        imageButton.setVisibility(View.VISIBLE);
    }

    //Metodo que inicia o cronometro apos inicar a queda
    public void startCronometer(){
        countDownTimer = new CountDownTimer(5*1000, 1000) {

            //Atualiza o textview para mostrar os segundos restantes
            public void onTick(long millisUntilFinished) {
                mTextView3.setText("" + millisUntilFinished / 1000);
                //here you can have your logic to set text to edittext
            }

            //Ao finalizar o countDown envia a mensagem para o node de queda detectada
            public void onFinish() {
                requestTranscription("Queda detectada".getBytes());
            }

        };

        //Variavel que inicia o cronometro
        countDownTimer.start();
    }

    private boolean isFallDetected(double x,
                                   double y,
                                   double z) {

        //Calcula aceleracao
        double acceleration = this.calculateSumVector(x, y, z);

        //Armazena dados os dados da leitura nas variaveis
        this.addAccelerometerValuesToList(x, y, z, acceleration);

        StringBuilder msg = new StringBuilder("x: ").append(x)
                .append(" y: ").append(y)
                .append(" z: ").append(z)
                .append(" acc: ").append(acceleration);
       Log.d("FDS-Acc-Values", msg.toString());

       //Veirifca se os dados lidos sao maiores que os limiares
        if (acceleration > CSV_THRESHOLD) {
            //Verifica o limiar da variacao do angulo
            double angleVariation = this.calculateAngleVariation();
            if (angleVariation > CAV_THRESHOLD) {
                //verifica o limiar da mudanca do angulo
                double changeInAngle = this.calculateChangeInAngle();
                if (changeInAngle > CCA_THRESHOLD) {
                    msg.append(System.currentTimeMillis());
                    //Retorna que um possivel evento de queda ocorreu
                    return true;
                }
            }
        }
        return false;
    }


    private void addAccelerometerValuesToList(double x,
                                              double y,
                                              double z,

                                              double acceleration) {
        //Armazena os dados de 200 leituras consecutivas o que corresponde a 4seg no tempo 200*50HZ = 4
        if(this.accelerometerValues.size() >= 200) {
            this.accelerometerValues.remove(0);
        }
        //Armazena os dados de 40 leituras consectuvias o que corresponde a 0.8 seg no tempo 40*50HZ = 0.8
        if(this.accelerometerValuesDesvPadrao.size() >= 40) {
            this.accelerometerValuesDesvPadrao.remove(0);
        }
       //Cria obeto com os dados da leitura
        Map<AccelerometerAxis, Double> map = new HashMap<>();
        map.put(AccelerometerAxis.X, x);
        map.put(AccelerometerAxis.Y, y);
        map.put(AccelerometerAxis.Z, z);
        map.put(AccelerometerAxis.ACCELERATION, acceleration);
        //Adiciona os valores na variavel
        this.accelerometerValuesDesvPadrao.add(map);
        this.accelerometerValues.add(map);
        //Caso uma queda seja detectada obter leitura dos proximos 0.4seg no tempo o que corresponde a 20 amostrar 20*50HZ = 0.4

        if (countOneQuarterseg){
            if (this.accelerometerValues04seg.size() >= 20){
                //Para a leitura apos obter as 20 amostrar apos a queda
                countOneQuarterseg = false;
                //Interrompe as leituras
                stopReadings();
                //Obtem o desvio padrao que é uma medida de dispersao em relacao a media. Este valor de 1.5*9.8 tem como base o trabalho do Victor Tavares
                //Caso o desvio padrao seja maior que esse valor detectamos que a queda realmente foi detectada
                if(getDesvPadrao() > 1.5 * 9.8){
                    //Vibra o relogio indicando a queda
                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    long[] vibrationPattern = {0, 500, 50, 300};

                    //-1 - don't repeat
                    final int indexInPatternToRepeat = -1;
                    vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
                    //Muda o layout da tela para mostrar que ocorreu a queda
                    setupFallLayout();
                    //Zera os valores do array
                    this.accelerometerValues04seg = new ArrayList<>();
//
                }else {
                    //Caso a queda nao tenha sido confirmada, retoma com as leituras
                    initSensor();
                }

            }
            else
                this.accelerometerValues04seg.add(map);
        }
    }

    //Metodo para calcular a aceleracao
    private double calculateSumVector(double x,
                                      double y,
                                      double z) {
        return Math.abs(x) + Math.abs(y) + Math.abs(z);
    }


    //Metodo para calcular a variacao no angulo
    private double calculateAngleVariation() {
        int size = this.accelerometerValues.size();
        if (size < 150){
            return -1;
        }

        //1 seg equivale a leitura de 50 amostras consecutivas na frequencia de 50Hz
        int seg = 50;
        //Obtem os dados de duas leituras com um intervalo de diferenca de 1 seg do acc dado que 1 seg = 50*50Hz
        Map<AccelerometerAxis, Double> minusTwo = this.accelerometerValues.get(size - 1*seg);
        Map<AccelerometerAxis, Double> minusOne = this.accelerometerValues.get(size - 1);

        //Calcula o valor da multiplicacao de dois vetores An*An1
        double anX = minusTwo.get(AccelerometerAxis.X) * minusOne.get(AccelerometerAxis.X);
        double anY = minusTwo.get(AccelerometerAxis.Y) * minusOne.get(AccelerometerAxis.Y);
        double anZ = minusTwo.get(AccelerometerAxis.Z) * minusOne.get(AccelerometerAxis.Z);
        double an = anX + anY + anZ;

        //Calcula o valor do modulo do vetor ||An|| dado que ||.|| = forma euclidiana = raiz da soma dos quadrados de cada componente
        double anX0 = Math.pow(minusTwo.get(AccelerometerAxis.X), 2);
        double anY0 = Math.pow(minusTwo.get(AccelerometerAxis.Y), 2);
        double anZ0 = Math.pow(minusTwo.get(AccelerometerAxis.Z), 2);
        double an0 = Math.sqrt(anX0 + anY0 + anZ0);

        //Calcula o valor do moudlo do vetor ||An1||
        double anX1 = Math.pow(minusOne.get(AccelerometerAxis.X), 2);
        double anY1 = Math.pow(minusOne.get(AccelerometerAxis.Y), 2);
        double anZ1 = Math.pow(minusOne.get(AccelerometerAxis.Z), 2);
        double an1 = Math.sqrt(anX1 + anY1 + anZ1);

        //Calcula o valor de (An*An1)/(||An||*||An1||)
        double a = an / (an0 * an1);

        //Calcula o cos inverso para obter o angulo e converte esse valor para radianos
        return Math.acos(a) * (180 / Math.PI);
    }

    //Metodo para calcular a mudanca no angulo
    private double calculateChangeInAngle() {
        int size = this.accelerometerValues.size();
        if (size < 200){
            return -1;
        }
        //Primeira corresponde a amostra inicial dentro dos 4seg de janela ||t = 0
        Map<AccelerometerAxis, Double> first = this.accelerometerValues.get(0);
        //Segunda amostra corresponde a amostra final dos 4seg de janela ||t = 4
        Map<AccelerometerAxis, Double> second = this.accelerometerValues.get(size - 1);

        //Ab * Ae
        double aX = first.get(AccelerometerAxis.X) * second.get(AccelerometerAxis.X);
        double aY = first.get(AccelerometerAxis.Y) * second.get(AccelerometerAxis.Y);
        double aZ = first.get(AccelerometerAxis.Z) * second.get(AccelerometerAxis.Z);

        double a0 = aX + aY + aZ;

        //Forma euclidiana
        //||Ab||*||Ae|| dado que ||.|| = forma euclidiana = raiz da soma dos quadrados de cada componente
        aX = Math.pow(aX, 2);
        aY = Math.pow(aY, 2);
        aZ = Math.pow(aZ, 2);
        double a1 = (Math.sqrt(aX) + Math.sqrt(aY) + Math.sqrt(aZ));

        //Calcula o cos inverso para obter o angulo e converte esse valor para radianos
        return Math.acos(a0 / a1) * (180 / Math.PI);
    }

    //Metodo para calcular o desvio padrao
    public Double getDesvPadrao(){
        Double media = getMedia();
        int tam = accelerometerValuesDesvPadrao.size();
        Double desvPadrao = 0D;
        for (Map<AccelerometerAxis, Double> map:accelerometerValuesDesvPadrao){
            Double vlr = map.get(AccelerometerAxis.ACCELERATION);
            Double aux = vlr - media;
            desvPadrao += aux * aux;
        }
        return Math.sqrt(desvPadrao / (tam));
    }

    //Metodo para calcular a media
    public Double getMedia(){

        Double soma = 0.0;
        for (Map<AccelerometerAxis, Double> map:accelerometerValuesDesvPadrao){
            soma += map.get(AccelerometerAxis.ACCELERATION);
        }

        return soma/accelerometerValuesDesvPadrao.size();
    }


    private void updateTranscriptionCapability(CapabilityInfo capabilityInfo) {
        //Obtem os nós conectados ao relogio
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        //Escolhe um node dentro os listados para enviar a mensagem
        transcriptionNodeId = pickBestNodeId(connectedNodes);

    }

    // Procura um node proximo ou escolhe um arbitrariamente
    private String pickBestNodeId(Set<Node> nodes) {
        String bestNodeId = null;

        for (Node node : nodes) {
            Log.d("node", "pickBestNodeId: " + node.getDisplayName());
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    //Método responsavel realizar a comunicação com o App
    private void requestTranscription(byte[] info) {
        if (transcriptionNodeId != null) {
            //Enviar a informação através do comunicação bluetooth entre o watch e o node escolhido
            Task<Integer> sendTask =
                    Wearable.getMessageClient(this).sendMessage(
                            transcriptionNodeId, FALL_CAPABILITY_NAME, info);
            // You can add success and/or failure listeners,
            // Or you can call Tasks.await() and catch ExecutionException
            sendTask.addOnSuccessListener(new OnSuccessListener<Integer>() {
                @Override
                public void onSuccess(Integer integer) {
                    Log.d("watch", "onSuccess: Deu certo" );
                }
            });
            sendTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("watch", "onSuccess: Deu ruim" );
                }
            });
        } else {
            // Unable to retrieve node with transcription capability
        }
    }

}
