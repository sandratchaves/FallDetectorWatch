package com.sandra.falldetector2;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.LocationRequest;

import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import com.sandra.falldetector2.repository.ContactRepository;

public class App extends Application {
    public static App instance;
    private SharedPreferences sharedPreferences;
    private ContactRepository contactRepository = new ContactRepository();
    private Handler handler;
    private Location location;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        setSharedPreferences(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Realm.init(this);
        locationUpdate();
    }

    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }

    public static App getInstance() {
        return instance;
    }


    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public void setSharedPreferences(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }


    //Método para obter a última localização conhecida do usuário
    public void getLastLocation() {

        //Verifica se o App tem as permissões necessárias para obter a localização e em caso positivo sai do método
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(this);
        //Méotod para obter a última localização conhecida do usuário
        locationProvider.getLastKnownLocation()
                .subscribe(location1 -> {
                    if (location1 != null) {
                        String baseLocation = "{\"latitude\":" + location1.getLatitude() + ", \"longitude\":" + location1.getLongitude() + "}";
                        Log.d("Location", "onCreate: " + baseLocation);
                        location = location1;
                    }
                });

        LocationRequest req = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setExpirationDuration(TimeUnit.SECONDS.toMillis(1000))
                .setInterval(1000);

        Observable<Location> goodEnoughQuicklyOrNothingObservable = locationProvider.getUpdatedLocation(req)
                .filter(new Func1<Location, Boolean>() {
                    @Override
                    public Boolean call(Location location) {
                        return location.getAccuracy() < 5;
                    }
                })
                .timeout(1000, TimeUnit.SECONDS, Observable.just((Location) null), AndroidSchedulers.mainThread())
                .first()
                .observeOn(AndroidSchedulers.mainThread());

        goodEnoughQuicklyOrNothingObservable.subscribe();

    }


    //Método para recuperar a localização do usuário em background a cada 60 segundos
    public void locationUpdate(){
        handler = new Handler();

        final Runnable r = new Runnable() {
            public void run() {
                getLastLocation();
                handler.postDelayed(this, 1000*60);
            }
        };

        handler.postDelayed(r, 1000);
    }

    public ContactRepository getContactRepository() {
        return contactRepository;
    }

    public Location getLocation() {
        return location;
    }
}