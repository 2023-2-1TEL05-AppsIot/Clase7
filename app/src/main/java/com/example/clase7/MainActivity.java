package com.example.clase7;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.clase7.databinding.ActivityMainBinding;
import com.example.clase7.entity.Job;
import com.example.clase7.entity.JobDto;
import com.example.clase7.repo.JobRepository;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    Job[] listaJobs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        JobRepository jobRepository = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080")
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(JobRepository.class);


        binding.btnSaveJson.setOnClickListener(view ->
                jobRepository.getJobs().enqueue(new Callback<JobDto>() {
                    @Override
                    public void onResponse(Call<JobDto> call, Response<JobDto> response) {

                        if (response.isSuccessful()) {
                            JobDto jobDto = response.body();
                            System.out.println("todo ok");
                            Job[] listaTrabajos = jobDto.get_embedded().getJobs();
                            for (Job job : listaTrabajos) {
                                Log.d("msg-test", job.getJobTitle());
                            }
                            guardarArchivoTextoComoJson(listaTrabajos);

                        } else {
                            Log.d("msg-test", "algo salió mal");
                        }
                    }

                    @Override
                    public void onFailure(Call<JobDto> call, Throwable t) {
                        t.printStackTrace();
                    }
                }));

        binding.btnSaveAsObject.setOnClickListener(view -> {
            fetchDataFromWebservice();
        });

        binding.btnReadJson.setOnClickListener(view -> leerArchivoTexto());

        binding.btnReadObj.setOnClickListener(view -> leerArchivoObjeto());

        binding.btnSaveSubdir.setOnClickListener(view -> {
            String fileName = "listaTrabajosJson";

            try (FileInputStream fileInputStream = openFileInput(fileName);
                 FileReader fileReader = new FileReader(fileInputStream.getFD());
                 BufferedReader bufferedReader = new BufferedReader(fileReader)) {

                String jsonData = bufferedReader.readLine();
                Gson gson = new Gson();

                guardarEnSubdirectorios(gson.fromJson(jsonData, Job[].class));


            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        binding.btnSaveTexto.setOnClickListener(view -> {

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, "archivo.txt");
            activityForResultLauncher.launch(intent);

        });

        binding.btnDownloadManager.setOnClickListener(view -> {

            String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;

            // >=29
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                descargarConDownloadManager();
            }else{
                launcher.launch(permission);
            }

        });

        binding.btnSharedPref.setOnClickListener(view -> {
            SharedPreferences preferences = getPreferences(MODE_PRIVATE);
            SharedPreferences.Editor edit = preferences.edit();

            edit.putInt("edad",20);
            edit.putString("orden","nombrePrimero");

            edit.apply();

        });

        binding.btnLeerSharedPref.setOnClickListener(view -> {
            SharedPreferences preferences = getPreferences(MODE_PRIVATE);
            int edad = preferences.getInt("edad",0);
            String orden = preferences.getString("orden","vacio");

            if(edad > 0 && !orden.equals("vacio")){
                Toast.makeText(this, "Edad: " + edad + " | orden: " + orden, Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, "No se encontraron shared pref", Toast.LENGTH_SHORT).show();
            }


        });
    }

    public void descargarConDownloadManager(){
        String fileName = "pucp.jpg";
        String url = "http://maternet.edu.pe/sites/default/files/images/LOGO-PUCP.jpg";

        //URL URi
        Uri downloadUri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(downloadUri);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        request.setAllowedOverRoaming(false);
        request.setTitle(fileName);
        request.setMimeType("image/jpeg");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, File.separator + fileName);

        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        try {
            dm.enqueue(request);
        }catch (RuntimeException e){
            e.printStackTrace();
        }

    }

    ActivityResultLauncher<String> launcher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {

                if (isGranted) { // permiso concedido
                    descargarConDownloadManager();
                } else {
                    Log.e("msg-test", "Permiso denegado");
                }
            });

    ActivityResultLauncher<Intent> activityForResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();

                    if (data != null) {
                        try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(data.getData(), "w");
                             FileWriter fileWriter = new FileWriter(pfd.getFileDescriptor())) {

                            String textoGuardar = binding.editTextText.getText().toString();
                            fileWriter.write(textoGuardar);
                            Toast.makeText(this, "Archivo guardado", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

    public void fetchDataFromWebservice() {

        JobRepository jobRepository = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080")
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(JobRepository.class);

        binding.btnSaveAsObject.setEnabled(false);

        jobRepository.getJobs().enqueue(new Callback<JobDto>() {
            @Override
            public void onResponse(Call<JobDto> call, Response<JobDto> response) {

                if (response.isSuccessful()) {
                    JobDto jobDto = response.body();
                    System.out.println("todo ok");
                    listaJobs = jobDto.get_embedded().getJobs();
                    binding.btnSaveAsObject.setEnabled(true);
                    if (listaJobs != null) {
                        guardarArchivoContenidoObjeto();
                    }
                } else {
                    Log.d("msg-test", "algo salió mal");
                }
            }

            @Override
            public void onFailure(Call<JobDto> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    public void guardarArchivoTextoComoJson(Job[] lista) {
        //convertimos el arreglo a un String (para guardarlo como json
        Gson gson = new Gson();
        String listaTrabajosJson = gson.toJson(lista);

        //nombre del archivo a guardar
        String fileNameJson = "listaTrabajosJson";

        //Se utiliza la clase FileOutputStream para poder almacenar en Android
        try (FileOutputStream fileOutputStream = openFileOutput(fileNameJson, Context.MODE_PRIVATE);
             FileWriter fileWriter = new FileWriter(fileOutputStream.getFD())) {

            fileWriter.write(listaTrabajosJson);

            Log.d("msg-test", "Archivo guardado correctamente");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void guardarArchivoContenidoObjeto() {

        //nombre del archivo a guardar
        String fileNameJson = "listaTrabajosObjeto";

        //Se utiliza la clase FileOutputStream para poder almacenar en Android
        try (FileOutputStream fileOutputStream = openFileOutput(fileNameJson, Context.MODE_PRIVATE);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {

            //con objectOutputStream se realiza la escritura como objeto
            objectOutputStream.writeObject(listaJobs);
            Log.d("msg-test", "Archivo guardado correctamente");

            Toast.makeText(this, "Archivo guardado", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listarArchivosGuardados() {
        String[] archivosGuardados = fileList();

        for (String archivo : archivosGuardados) {
            Log.d("msg-test", archivo);
            if (deleteFile(archivo)) {
                Log.d("msg-test", "archivo: " + archivo + " borrado");
            } else {
                Log.d("msg-test", "ocurrió un error al borrar el archivo");
            }
        }
    }

    public void leerArchivoTexto() {
        String fileName = "listaTrabajosJson";

        try (FileInputStream fileInputStream = openFileInput(fileName);
             FileReader fileReader = new FileReader(fileInputStream.getFD());
             BufferedReader bufferedReader = new BufferedReader(fileReader)) {

            String jsonData = bufferedReader.readLine();

            Gson gson = new Gson();
            Job[] listaTrabajos = gson.fromJson(jsonData, Job[].class);


            for (Job j : listaTrabajos) {
                Log.d("msg-test", "nombre trabajo: " + j.getJobTitle());
            }

            //List<Job> jobList = Arrays.asList(listaTrabajos);
            //Job job = new Job();
            //job.setJobTitle("ZZZZZZZ");
            //jobList.add(job);

            //Job[] l = (Job[]) jobList.toArray();

            //guardarArchivoTextoComoJson(l);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void leerArchivoObjeto() {
        String fileName = "listaTrabajosObjeto";

        Log.d("msg-test", "----- lectura de objetos -----");

        try (FileInputStream fileInputStream = openFileInput(fileName);
             ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {

            Job[] arregloTrabajos = (Job[]) objectInputStream.readObject();

            for (Job j : arregloTrabajos) {
                Log.d("msg-test", j.getJobTitle());
            }

        } catch (FileNotFoundException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Log.d("msg-test", "----- fin de lectura de objetos -----");
    }

    public void guardarEnSubdirectorios(Job[] lista) {

        String fileNameJson = "trabajosComoJsonEnSubDir";

        File subFolder = new File(getFilesDir(), "subdirectory");

        if (!subFolder.exists()) {
            subFolder.mkdir();
        }

        // /files/subdirectory/trabajosComoJsonEnSubDir <- este es el archivo traba....
        File file = new File(subFolder, fileNameJson);

        /*
        //   /files/u1/historias/chiste1.txt
        File s1 = new File(getFilesDir(),"u1");
        File s2 = new File(s1,"historias");
        File chis = new File(s2,"chiste1.txt"); */

        try (FileOutputStream outputStream = new FileOutputStream(file);
             FileWriter fileWriter = new FileWriter(outputStream.getFD())) {

            Gson gson = new Gson();
            String listaTrabajosJson = gson.toJson(lista);
            fileWriter.write(listaTrabajosJson);
            Log.d("msg-test", "archivo guardado en subcarpeta!");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }


}