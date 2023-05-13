import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        try (Socket socket = new Socket(Main.HOST, Main.PORT);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            try (Scanner scanner = new Scanner(System.in)) {
                System.out.println("Пожалуйста введите слова для поиска через пробел");

                //вводим и отправляем слово для поиска по pdf файлам
                writer.println(scanner.nextLine());
            }
            //получаем в ответ json строку
            System.out.println(reader.readLine());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
