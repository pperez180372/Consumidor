package org.muinf.seu.pperez.consumidor;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.ProtocolException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;


public class MainActivity extends ActionBarActivity {

    MainActivity myself;



    float [] buffer=new float[3600];
    int puntero_vector=0;
    int num_muestras=0;
    Canvas grafica_1;
    Canvas grafica_2;
    int canvas_visible;


    Paint paintred;
    Paint paintwhite;
    BitmapDrawable BitmapD_1;
    BitmapDrawable BitmapD_2;


    int mx,Mx,my,My;
    int bmpdx=200,bmpdy=100;



    Button Botonsuscribir;
    Button Botonactualizar;
    Button Botondesconectar;



    public QueryContextTask ay;
    public unsubscribeContextTask ayy;
    public subscribeContextTask ayyy;
    String nameentity;
    int puerto_de_escucha=54545;
    ServerSocket sk;


    /**
     * + * Returns a valid InetAddress to use for RMI communication. + * If the
     * system property java.rmi.server.hostname is set it is used. + * Secondly
     * InetAddress.getLocalHost is used. + * If neither of these are
     * non-loopback all network interfaces + * are enumerated and the first
     * non-loopback ipv4 + * address found is returned. If that also fails null
     * is returned. +
     */
    private static InetAddress getLocalAddress() {
        InetAddress inetAddr = null;

        /**
         * 1) If the property java.rmi.server.hostname is set and valid, use it
         */
        try {
            System.out
                    .println("Attempting to resolve java.rmi.server.hostname");
            String hostname = System.getProperty("java.rmi.server.hostname");
            if (hostname != null) {
                inetAddr = InetAddress.getByName(hostname);
                if (!inetAddr.isLoopbackAddress()) {
                    return inetAddr;
                } else {
                    System.out
                            .println("java.rmi.server.hostname is a loopback interface.");
                }

            }
        } catch (SecurityException e) {
            System.out
                    .println("Caught SecurityException when trying to resolve java.rmi.server.hostname");
        } catch (UnknownHostException e) {
            System.out
                    .println("Caught UnknownHostException when trying to resolve java.rmi.server.hostname");
        }

        /** 2) Try to use InetAddress.getLocalHost */
        try {
            System.out
                    .println("Attempting to resolve InetADdress.getLocalHost");
            InetAddress localHost = null;
            localHost = InetAddress.getLocalHost();
            if (!localHost.isLoopbackAddress()) {
                return localHost;
            } else {
                System.out
                        .println("InetAddress.getLocalHost() is a loopback interface.");
            }

        } catch (UnknownHostException e1) {
            System.out
                    .println("Caught UnknownHostException for InetAddress.getLocalHost()");
        }

        /** 3) Enumerate all interfaces looking for a candidate */
        Enumeration ifs = null;
        try {
            System.out
                    .println("Attempting to enumerate all network interfaces");
            ifs = NetworkInterface.getNetworkInterfaces();

            // Iterate all interfaces
            while (ifs.hasMoreElements()) {
                NetworkInterface iface = (NetworkInterface) ifs.nextElement();

                // Fetch all IP addresses on this interface
                Enumeration ips = iface.getInetAddresses();

                // Iterate the IP addresses
                while (ips.hasMoreElements()) {
                    InetAddress ip = (InetAddress) ips.nextElement();
                    if ((ip instanceof Inet4Address) && !ip.isLoopbackAddress()) {
                        return (InetAddress) ip;
                    }
                }
            }
        } catch (SocketException se) {
            System.out.println("Could not enumerate network interfaces");
        }

        /** 4) Epic fail */
        System.out
                .println("Failed to resolve a non-loopback ip address for this host.");
        return null;
    }





    synchronized void imprimir(final String cad) {
        final String g = cad;

        runOnUiThread(new Runnable() {
            public void run() {
                TextView ll = (TextView) findViewById(R.id.editResultado);
                if (ll!=null)  ll.setText(cad);


            }
        });
    }
    public void imprimirln(final String cad) {
        imprimir(cad+"\r\n");
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myself=this;


        /* crea el canvas para poder dibujar la gráfica */

        paintred = new Paint();
        paintred.setColor(Color.parseColor("#CD5C5C"));
        paintred.setStyle(Paint.Style.FILL_AND_STROKE);
        paintred.setAntiAlias(false);
        paintwhite = new Paint();
        paintwhite.setColor(Color.parseColor("#FFFFFF"));


        Bitmap bg_1 = Bitmap.createBitmap(bmpdx, bmpdy, Bitmap.Config.ARGB_8888);
        Bitmap bg_2 = Bitmap.createBitmap(bmpdx, bmpdy, Bitmap.Config.ARGB_8888);
        grafica_1 = new Canvas(bg_1);
        grafica_2 = new Canvas(bg_2);
        BitmapD_1=new BitmapDrawable(getResources(),bg_1);
        BitmapD_2=new BitmapDrawable(getResources(),bg_2);
        canvas_visible=1;
        FrameLayout ll = (FrameLayout) findViewById(R.id.Grafica);
        ll.setBackgroundDrawable(BitmapD_1);


        my=15; // temperatura minima;
        My=40; //temperatura máxima;
        mx=0;  // tiempo minimo; en segundos // dependerá del numero de muestras
        Mx=60;



        // Ejemplo de empleo funcion nuevamuestra crear un hilo que invoque a nuevamuestra para representar

        /*new Thread(new Runnable() {
            public void run() {int x=0;
               imprimir("Hilo de dibujo Arrancado");

                while(true){ nuevamuestra((float) (Math.random()*0.0+25.0+10.0*Math.sin(2.0*Math.PI*(float)(x%360)/360.0)));
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    x++;
                }
            }
        }).start();*/


        // hilo de subscripción

        new Thread(new Runnable() {
            public void run() {
                // cliente echo

                while (true)
                {
                try {
                    while (true)
                    {
                      /*  java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
                        String g=addr.getHostAddress();*/
                        InetAddress g1=getLocalAddress();
                        sk = new ServerSocket(puerto_de_escucha,0,g1);
                        StringBuilder     sb=null,sbj=null;

                        while (!sk.isClosed()) {
                            sbj=null;
                            sb=null;
                            Socket cliente = sk.accept();
                            BufferedReader entrada = new BufferedReader(
                                    new InputStreamReader(cliente.getInputStream()));
                            PrintWriter salida = new PrintWriter(
                                    new OutputStreamWriter(cliente.getOutputStream()), true);
                            String line = null;
                            sb= new StringBuilder();
                            try {
                                do {
                                    line = entrada.readLine();
                                    sb.append(line + "\n");
                                    //hacking
                                    if ((sbj==null)&&(line.contains("{"))) // date
                                        sbj= new StringBuilder();
                                    if (sbj!=null)
                                    {
                                        sbj.append(line+"\n");
                                    }
                                    System.out.println(""+sb);
                                }
                                while (entrada.ready());
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                            //String datos = entrada.readLine();

                            System.out.println("Fin de lectura");
                            JSONObject jObject = null;
                            try {
                                jObject = new JSONObject(sbj.toString());

                                JSONArray jArray = jObject.getJSONArray("contextResponses");
                                JSONObject c = jArray.getJSONObject(0);
                                JSONObject jo = c.getJSONObject("contextElement");
                                String name = jo.getString("id");

                                if (nameentity!=null)
                                {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((TextView)  findViewById(R.id.editResultado)).setText(nameentity);
                                        }
                                    });

                                if (name.contains(nameentity))
                                {

                                    JSONArray jArrayattr = jo.getJSONArray("attributes");
                                for(int i = 0; i <  jArrayattr.length(); i++) {
                                    try {
                                        JSONObject ca = jArrayattr.getJSONObject(i);

                                        if (ca.getString("name").contains("temperature")) {

                                            final double va = ca.getDouble("value");
                                            nuevamuestra((float)va);

                                           /* runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    EditText t= (EditText)  findViewById(R.id.editTemperatura);

                                                    t.setText("" + va);

                                                }
                                            });*/


                                        }

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                } // del for
                                }
                                }


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                            salida.println("POST HTTP/1.1 200");
                            cliente.close();
                        }
                        Thread.sleep(1000);

                    }
                } catch (IOException e) {
                    System.out.println(e);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }




            }
        }

        }).start();


        Botonactualizar = (Button) findViewById(R.id.buttonActualizar);
        Botonactualizar.setOnClickListener(               new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nameentity=((TextView)(findViewById(R.id.editEntidad))).getText().toString();
                ay=new QueryContextTask();
                ay.execute("");

            };
        });

        Botondesconectar = (Button) findViewById(R.id.buttonDesconectar);
        Botondesconectar.setOnClickListener(               new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ayy=new unsubscribeContextTask();
                ayy.execute("");

                nameentity=null;
            };
        });

        Botonsuscribir = (Button) findViewById(R.id.buttonSubscribir);
        Botonsuscribir.setOnClickListener(               new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                nameentity=((TextView)(findViewById(R.id.editEntidad))).getText().toString();
                ayyy=new subscribeContextTask();
                ayyy.execute("");

            };
        });



    }


    public void nuevamuestra(float dato)
    {
  /*       int w,h;


         /*FrameLayout ll = (FrameLayout) findViewById(R.id.Grafica);
         w=ll.getWidth();
         h=ll.getHeight();
*/

        final float datum=dato;
        final int num_muestrum=num_muestras++;

        Canvas grafica;
        canvas_visible=(canvas_visible+1)%2;
        if (canvas_visible==1)
            grafica=grafica_1;
        else
            grafica=grafica_2;
        // borrar esta escalado.
        grafica.drawRect(0, 0, bmpdx, bmpdy, paintwhite);
        float sy= (float) (bmpdy/(My-my)); // en grados
        float sx=(float) bmpdx/(Mx-mx);   // en segundos

        //añadir dato
        buffer[puntero_vector++]=dato;
        if ((puntero_vector>=Mx))
        {
            //imprimirln("Ha finalidado el la peusta de datos");
            puntero_vector=0;
        }

        //   if ((puntero_vector%10)==0)
        {
            // grafica de lineas no se escala la x
            int t;
            for (t=0;t<Mx-1;t++)
            {
                float va,va1;
                va=buffer[t];
                if (va>My) va=My;
                if (va<my) va=my;
                va1=buffer[t+1];
                if (va1>My) va1=My;
                if (va1<my) va1=my;
                float x1,y1,x2,y2;

                x1=t*sx;
                y1=bmpdy-(va-my)*sy;
                x2=(t+1)*sx;
                y2=bmpdy-(va1-my)*sy;

                grafica.drawLine(x1,y1,x2,y2, paintred);

            }



            if (canvas_visible == 1) {
                runOnUiThread(new Runnable() {
                    public void run() {

                        TextView t= (TextView)  findViewById(R.id.editTemperatura);
                        t.setText("" + datum);
                        t= (TextView)  findViewById(R.id.editnmuestras);
                        t.setText("" + num_muestrum);
                        FrameLayout ll = (FrameLayout) findViewById(R.id.Grafica);
                        ll.setBackgroundDrawable(BitmapD_1);
                        // ll.postInvalidate();

                    }
                });

            } else {
                runOnUiThread(new Runnable() {
                    public void run() {
                        TextView t= (TextView)  findViewById(R.id.editTemperatura);
                        t.setText("" + datum);
                        t= (TextView)  findViewById(R.id.editnmuestras);
                        t.setText("" + num_muestrum);
                        FrameLayout ll = (FrameLayout) findViewById(R.id.Grafica);
                        ll.setBackgroundDrawable(BitmapD_2);

//                     ll.postInvalidate();

                    }
                });
            }
        }



    }


    class QueryContextTask extends AsyncTask<String, Void, String> {

        String res;

        protected String doInBackground(String... urls) {

            Map<String, List<String>> rr;
            String res = "";
            InputStream is = null;
            // Only display the first 500 characters of the retrieved
            // web page content.
            int len = 500;
            // View rootView = findViewById(R.layout.fragment_main);


            EditText tEntidad;
            TextView tHumedad;
            TextView tTemperatura;

            tEntidad = (EditText) findViewById(R.id.editEntidad);
            //tHumedad=  (EditText)  rootView.findViewById(R.id.editHumedad);
            tTemperatura=  (TextView)  findViewById(R.id.editTemperatura);

            //String username = tuser.getText().toString(); //"Android_SEU_3n5_1";//
            //String passwd = tpassword.getText().toString(); // "sensor";//
            //String domain = tdomain.getText().toString(); // "Asignatura SEU";

            String HeaderAccept = "application/json";
            String HeaderContent = "application/json";
            String payload = "{\"entities\": [{\"type\": \"Sensor\",\"isPattern\": \"false\",\"id\": \""+nameentity+"\"}]}";
            // String encodedData = URLEncoder.encode(payload, "UTF-8");
            // String encodedData = payload;
            String leng = null;
            String resp=        "none";

            try {
                leng = Integer.toString(payload.getBytes("UTF-8").length);

                OutputStreamWriter wr = null;
                BufferedReader rd = null;
                StringBuilder sb = null;


                URL url = null;

                url = new URL("http://pperez-seu-or.disca.upv.es:1026/v1/queryContext");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000); // miliseconds
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");

                conn.setRequestProperty("Accept", HeaderAccept);
                conn.setRequestProperty("Content-type", HeaderContent);
                //conn.setRequestProperty("Fiware-Service", HeaderService);
                conn.setRequestProperty("Content-Length", leng);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(payload.getBytes("UTF-8"));
                os.flush();
                os.close();


                int rc = conn.getResponseCode();

                resp = conn.getContentEncoding();
                is = conn.getInputStream();

                if (rc == 200) {

                    resp = "OK";
                    //read the result from the server
                    rd = new BufferedReader(new InputStreamReader(is));
                    sb = new StringBuilder();

                    String line = null;
                    while ((line = rd.readLine()) != null)
                    {
                        sb.append(line + "\n");
                    }
                    String result = sb.toString();


                    JSONObject jObject = null;
                    try {
                        jObject = new JSONObject(result);

                        JSONArray jArray = jObject.getJSONArray("contextResponses");
                        JSONObject c = jArray.getJSONObject(0);
                        JSONObject jo = c.getJSONObject("contextElement");
                        JSONArray jArrayattr = jo.getJSONArray("attributes");

                        for(int i = 0; i <  jArrayattr.length(); i++) {
                            try {
                                JSONObject ca = jArrayattr.getJSONObject(i);

                                if (ca.getString("name").contains("temperature")) {

                                    final double va = ca.getDouble("value");
                                    nuevamuestra((float) va);

                                    /*runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((TextView)  findViewById(R.id.editTemperatura)).setText("" + va);

                                        }
                                    });*/


                                }


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } // del for



                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                    // cabeceras de recepcion
                    rr = conn.getHeaderFields();
                    System.out.println("headers: " + rr.toString());

                } else {
                    resp = "ERROR de conexión";
                    System.out.println("http response code error: " + rc + "\n");

                }


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            return resp;
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            res=result;
            TextView ttoken =   (TextView)  findViewById(R.id.editResultado);
            ttoken.setText(result);
        }
    }

    class unsubscribeContextTask extends AsyncTask<String, Void, String> {

        String res;

        protected String doInBackground(String... urls) {

            Map<String, List<String>> rr;
            String res = "";
            InputStream is = null;
            // Only display the first 500 characters of the retrieved
            // web page content.
            int len = 500;
            // View rootView = findViewById(R.layout.fragment_main);


            EditText tEntidad;
            TextView tHumedad;
            TextView tidsub;

            tidsub= (TextView) findViewById(R.id.editIdSubscripcion);

            //String username = tuser.getText().toString(); //"Android_SEU_3n5_1";//
            //String passwd = tpassword.getText().toString(); // "sensor";//
            //String domain = tdomain.getText().toString(); // "Asignatura SEU";

            String HeaderAccept = "application/json";
            String HeaderContent = "application/json";
            String payload =  "{\"subscriptionId\" : \""+tidsub.getText().toString()+"\"}";
            // String encodedData = URLEncoder.encode(payload, "UTF-8");
            // String encodedData = payload;
            String leng = null;
            try {
                leng = Integer.toString(payload.getBytes("UTF-8").length);

                OutputStreamWriter wr = null;
                BufferedReader rd = null;
                StringBuilder sb = null;


                URL url = null;

                url = new URL("http://pperez-seu-or.disca.upv.es:1026/v1/unsubscribeContext");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 ); // miliseconds
                conn.setConnectTimeout(15000 );
                conn.setRequestMethod("POST");

                conn.setRequestProperty("Accept", HeaderAccept);
                conn.setRequestProperty("Content-type", HeaderContent);
                //conn.setRequestProperty("Fiware-Service", HeaderService);
                conn.setRequestProperty("Content-Length", leng);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(payload.getBytes("UTF-8"));
                os.flush();
                os.close();


                int rc = conn.getResponseCode();
                String resp = conn.getContentEncoding();
                is = conn.getInputStream();

                if (rc == 200) {

                    resp = "OK";



                    //read the result from the server
                    rd = new BufferedReader(new InputStreamReader(is));
                    sb = new StringBuilder();

                    String line = null;
                    while ((line = rd.readLine()) != null)
                    {
                        sb.append(line + "\n");
                    }
                    String result = sb.toString();


                    JSONObject jObject = null;
                    try {
                        jObject = new JSONObject(result);

                        JSONObject jo = jObject.getJSONObject("statusCode");

                        final String err= jo.getString("reasonPhrase");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView)  findViewById(R.id.editIdSubscripcion)).setText(err);


                            }
                        });



                    } catch (JSONException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView)  findViewById(R.id.editIdSubscripcion)).setText("Error parsing json");


                            }
                        });
                        e.printStackTrace();
                    }






                } else {
                    resp = "ERROR de conexión";
                    System.out.println("http response code error: " + rc + "\n");

                }



                return resp;


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            return "error";
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            res=result;
            TextView ttoken =   (TextView)  findViewById(R.id.editResultado);
            ttoken.setText(result);
        }
    }
    class subscribeContextTask extends AsyncTask<String, Void, String> {

        String res;

        protected String doInBackground(String... urls) {

            Map<String, List<String>> rr;
            String res = "";
            InputStream is = null;
            // Only display the first 500 characters of the retrieved
            // web page content.
            int len = 500;
            // View rootView = findViewById(R.layout.fragment_main);


            EditText tEntidad;
            TextView tHumedad;
            TextView tTemperatura;

            tEntidad = (EditText) findViewById(R.id.editEntidad);
            //tHumedad=  (EditText)  rootView.findViewById(R.id.editHumedad);
            tTemperatura = (TextView) findViewById(R.id.editTemperatura);

            //String username = tuser.getText().toString(); //"Android_SEU_3n5_1";//
            //String passwd = tpassword.getText().toString(); // "sensor";//
            //String domain = tdomain.getText().toString(); // "Asignatura SEU";

            String HeaderAccept = "application/json";
            String HeaderContent = "application/json";

            if (sk == null)
                return "error socket no abierto";

            String urlEs = "http://" + sk.getInetAddress().getHostAddress() + ":" + sk.getLocalPort();
            String payload = "{\"entities\" : [{\"type\": \"Sensor\",\"isPattern\": \"false\",\"id\": \""+nameentity+"\"}],\"attributes\": [\"temperature\"], \"reference\": \"" + urlEs + "\", \"duration\": \"P1M\",\"notifyConditions\": [{    \"type\": \"ONCHANGE\", \"condValues\": [\"temperature\" ] } ], \"throttling\": \"PT5S\"}";

            // String encodedData = URLEncoder.encode(payload, "UTF-8");
            // String encodedData = payload;
            String leng = null;
            try {
                try {
                    leng = Integer.toString(payload.getBytes("UTF-8").length);
                } catch (UnsupportedEncodingException e1) {
                    e1.printStackTrace();
                }

                OutputStreamWriter wr = null;
                BufferedReader rd = null;
                StringBuilder sb = null;


                URL url = null;

                url = new URL("http://pperez-seu-or.disca.upv.es:1026/v1/subscribeContext");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000); // miliseconds
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");

                conn.setRequestProperty("Accept", HeaderAccept);
                conn.setRequestProperty("Content-type", HeaderContent);
                //conn.setRequestProperty("Fiware-Service", HeaderService);
                conn.setRequestProperty("Content-Length", leng);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(payload.getBytes("UTF-8"));
                os.flush();
                os.close();


                int rc = conn.getResponseCode();
                String resp = conn.getContentEncoding();
                is = conn.getInputStream();

                if (rc == 200) {

                    resp = "OK";
                    //read the result from the server
                    rd = new BufferedReader(new InputStreamReader(is));
                    sb = new StringBuilder();

                    String line = null;
                    while ((line = rd.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    String result = sb.toString();


                    JSONObject jObject = null;
                    try {
                        jObject = new JSONObject(result);

                        JSONObject jo = jObject.getJSONObject("subscribeResponse");
                        final String subsID = jo.getString("subscriptionId");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView) findViewById(R.id.editIdSubscripcion)).setText(subsID);


                            }
                        });


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                } else {
                    resp = "ERROR de conexión";
                    System.out.println("http response code error: " + rc + "\n");

                }

                return resp;


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            return "error";
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



}
