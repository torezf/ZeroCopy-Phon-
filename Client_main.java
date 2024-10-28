
package Main_ZeroCopy;


import java.util.Scanner;
import java.io.*;
import java.net.*;

import java.nio.channels.*;

public class Client_main {
    public static void main(String[] args) {
        String serverDistination = "192.168.1.173"; // connect to another device.
        String clientFilePath = "C:\\Users\\asus\\Desktop\\test\\22";
        int port = 20000;
        try {
            Socket socket = new Socket(serverDistination, port);
            System.out.println("server is connected.");
            new Server_handler(socket, clientFilePath, port, serverDistination).start();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class Server_handler extends Thread{
    private Socket socket;
    private String clientFilePath;
    private String serverDistination;
    private int port;
    private InputStream inputStream;
    private OutputStream outputStream;
    private BufferedReader reader;
    private Scanner scan;

    public Server_handler(Socket socket, String clientFilePath,int port, String serverDistination) throws Exception{
        this.socket = socket;
        this.clientFilePath =clientFilePath;
        this.outputStream = socket.getOutputStream();
        this.inputStream = socket.getInputStream();
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
        this.scan = new Scanner(System.in);
        this.port = port;
        this.serverDistination = serverDistination;
    }

    public void run(){
        String recieveMassage;
        try{
            while (true) {
                System.out.println(reader.readLine());
                if(!reader.ready()){
                    break;
                }
            }
            while (true) {
                String message = scan.nextLine() + "\n";
                if(message.trim().equalsIgnoreCase("exit")){
                    socket.close();
                    break;
                }
                
                sendMessageTOServer(message);

                while (true) {
                    recieveMassage = reader.readLine();
                    System.out.println(recieveMassage);
                    if(recieveMassage.trim().equals("server ready 1")){
                        String fileNameFromServer = reader.readLine();
                        System.out.println("client start to download use Singlethread: " + fileNameFromServer);
                        long start = System.currentTimeMillis();
                        SinglethreadreceiveFile(clientFilePath + "\\" + fileNameFromServer);
                        long end = System.currentTimeMillis();
                        System.out.println("Time Used : " + (end - start) + " ms.");
                    }
                    else if(recieveMassage.trim().equals("server ready 2")){
                        String fileNameFromServer = reader.readLine();
                        System.out.println("client start to download use Multithread: " + fileNameFromServer);
                        long start = System.currentTimeMillis();
                        MultithreadreceiveFile(clientFilePath + "\\" + fileNameFromServer);
                        long end = System.currentTimeMillis();
                        System.out.println("Time Used : " + (end - start) + " ms.");
                    }
                    else if(recieveMassage.trim().equals("server ready 3")){
                        String fileNameFromServer = reader.readLine();
                        System.out.println("client start to download use ZeroCopy: " + fileNameFromServer);
                        Thread.sleep(5);
                        long start = System.currentTimeMillis();
                        ZeroCopyreceiveFile(clientFilePath + "\\" + fileNameFromServer);
                        long end = System.currentTimeMillis();
                        System.out.println("Time Used : " + (end - start) + " ms.");
                    }
                    else if(recieveMassage.trim().equals("server ready 4")){
                        String fileNameFromServer = reader.readLine();
                        long[] alltime = new long[3];
                        alltime[0] = System.currentTimeMillis();
                        SinglethreadreceiveFile(clientFilePath + "\\" + "Single-thread-" + fileNameFromServer);
                        alltime[0] = System.currentTimeMillis() - alltime[0];
                        
                        sendMessageTOServer("clear 1\n");
                        alltime[1] = System.currentTimeMillis();
                        MultithreadreceiveFile(clientFilePath + "\\" + "Multithread-" + fileNameFromServer);
                        alltime[1] = System.currentTimeMillis() - alltime[1];
                        
                        sendMessageTOServer("clear 2\n");
                        Thread.sleep(5);
                        alltime[2] = System.currentTimeMillis();
                        ZeroCopyreceiveFile(clientFilePath + "\\" + "ZeroCopy-" + fileNameFromServer);
                        alltime[2] = System.currentTimeMillis() - alltime[2];
                        
                        System.out.println("\n\n\nTime used");
                        System.out.println("Single-Thread : "+ ((double)alltime[0]) / 1000 + " seconds");
                        System.out.println("Multihread : "+ ((double)alltime[1]) / 1000 + " seconds");
                        System.out.println("ZeroCopy : "+ ((double)alltime[2]) / 1000 + " seconds");
                    }
                    
                    if(!reader.ready()){
                        break;
                    }
                }
                
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessageTOServer(String message) throws IOException{
        outputStream.write(message.getBytes());
        outputStream.flush();
    }

    public void ZeroCopyreceiveFile(String saveFileName) throws IOException{
        File file = new File(saveFileName);
        file.createNewFile();
        
        SocketChannel socketChannel = null;
        RandomAccessFile randomAccessFile = null;
        FileChannel fileChannel = null;
        try{
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(serverDistination, 30000));
            System.out.println("connected to server!");

            randomAccessFile = new RandomAccessFile(saveFileName, "rw");
            fileChannel = randomAccessFile.getChannel();

            long pos = 0;
            long byteToRead;
            while(true){
                byteToRead = fileChannel.transferFrom(socketChannel, pos, Long.MAX_VALUE);
                if (byteToRead == 0) {
                    break;
                }
                pos += byteToRead;
            }
            System.out.println("transfer using ZeroCopy Complete");
            System.out.println("File received and saved at : " + saveFileName);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally{
            socketChannel.close();
            fileChannel.close();
            randomAccessFile.close();
        }
    }

    public void SinglethreadreceiveFile(String saveFileName) throws IOException{
        File file = new File(saveFileName);
        file.createNewFile();
        try(FileOutputStream fileOutputStream = new FileOutputStream(saveFileName)){
            

            byte[] buffer = new byte[1024];
            int bytesRead;

            // Read file data from the server
            long length = Long.parseLong(reader.readLine().trim());
            // System.out.println("len = " + length);

            long currentLength = 0;
            while (true) {
                bytesRead = inputStream.read(buffer);
                if(bytesRead <= 0){
                    break;
                }
                fileOutputStream.write(buffer, 0, bytesRead);
                currentLength += bytesRead;
                System.out.print("\rcurrent dowload : " + currentLength/(1024*1024) + "/" + length/(1024*1024) + " MB");
                if(currentLength == length){
                    break;
                }
            }
            System.out.println("\rtransfer using Single-thread Complete");
            System.out.println("File received and saved at : " + saveFileName);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void MultithreadreceiveFile(String saveFileName) throws IOException{
        File file = new File(saveFileName);
        file.createNewFile();

        long length = Long.parseLong(reader.readLine().trim());
        
        int threadCount = 10;
        long chunkSize = length / threadCount;
        RecieveThread[] recieveThreads = new RecieveThread[threadCount];
        for(int i=0;i<threadCount;i++){
            long start = i * chunkSize;
            long end;
            if(i == threadCount - 1){
                end = length;
            }
            else{
                end = start + chunkSize;
            }
            recieveThreads[i] = new RecieveThread(port + i + 1, start, end, saveFileName, serverDistination);
        }
        System.out.println("all thread to recieve is created.");
        String dummy = reader.readLine();
        System.out.println(dummy);
        for(int i=0;i<threadCount;i++){
            recieveThreads[i].start();
        }
        while(true){
            int threadClosed = 0;
            for(int i=0;i<threadCount;i++){
                if(!recieveThreads[i].isAlive()){
                    threadClosed ++;
                }
            }
            if(threadClosed == threadCount){
                break;
            }
        }
        System.out.println("transfer using Multithread Complete");
        System.out.println("File received and saved at : " + saveFileName);
    }
}

class RecieveThread extends Thread{
    private int port;
    private long start, end;
    private String filePath, serverDistination;
    private Socket socket;
    
    public RecieveThread(int port, long start, long end, String filePath, String serverDistination) throws IOException{
        this.port = port;
        this.start = start;
        this.end = end;
        this.filePath = filePath;
        this.serverDistination = serverDistination;
    }

    public void run(){
        try(RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "rw")){
            socket = new Socket(this.serverDistination, this.port);
            System.out.println("Client: Thread in port " + this.port + " is connected to server.");
            InputStream inputStream = socket.getInputStream();
            
            randomAccessFile.seek(start);

            byte[] buffer = new byte[4096];
            long totalRead = end - start;
            int bytesRead;
            while(true){
                if(totalRead > buffer.length){
                    bytesRead = inputStream.read(buffer, 0, buffer.length);
                }
                else{
                    bytesRead = inputStream.read(buffer, 0, (int)totalRead);
                }
                if(totalRead == 0){
                    break;
                }
                totalRead -= bytesRead;
                randomAccessFile.write(buffer, 0, bytesRead);
            }
            System.out.println("Client: Thread in port " + this.port + " recieve complete.");
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally{
            try{
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}