import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class BooleanSearchEngine implements SearchEngine {
    public static final String STOP_FILE = "stop-ru.txt";

    //в этой мапе будем хранить результаты сканирования всех наших файлов, в качестве ключа - слово для поиска
    private final Map<String, List<PageEntry>> cache;

    //список для стоп слов из txt
    private final List<String> stopWords;

    public BooleanSearchEngine(File pdfsDir) throws IOException {

        cache = new HashMap<>();
        stopWords = saveWordsFromStopFile();

        if (pdfsDir.isDirectory()) {
            for (File pdf : Objects.requireNonNull(pdfsDir.listFiles())) {

                if (pdf.isFile()) {

                    PdfDocument doc = new PdfDocument(new PdfReader(pdf));

                    //узнаем сколько страниц и проходимся по ним всем с 1ой
                    for (int pageNumber = 1; pageNumber <= doc.getNumberOfPages(); pageNumber++) {

                        //получаем объект одной страницы
                        PdfPage page = doc.getPage(pageNumber);

                        //получаем текст с каждой страницы
                        String textFromPage = PdfTextExtractor.getTextFromPage(page);

                        //делим на слова
                        String[] wordsFromPage = textFromPage.split("\\P{IsAlphabetic}+");

                        //надо посчитать слова на странице игнорируя регистр букв
                        //мапа, где ключом будет слово, а значением - частота
                        Map<String, Integer> frequencyOnPage = new HashMap<>();

                        countingWordsOnOnePage(wordsFromPage, frequencyOnPage);

                        //для каждого слова с этой страницы, сохраняем в общий кеш
                        saveIntoCache(pdf, pageNumber, frequencyOnPage);
                    }
                }
            }
        }
    }

    private List<String> saveWordsFromStopFile() {

        List<String> stopWords = new ArrayList<>();

        File stopFile = new File(STOP_FILE);

        if (stopFile.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(stopFile))) {

                //считываем все слова из файла
                String buff;
                while ((buff = reader.readLine()) != null) {

                    stopWords.add(buff);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return stopWords;
    }

    private void countingWordsOnOnePage(String[] wordsFromPage, Map<String, Integer> frequencyOnPage) {
        for (var word : wordsFromPage) { // перебираем слова
            if (word.isEmpty()) {
                continue;
            }
            word = word.toLowerCase();
            frequencyOnPage.put(word, frequencyOnPage.getOrDefault(word, 0) + 1);
        }
    }

    private void saveIntoCache(File pdf, int pageNumber, Map<String, Integer> frequencyOnPage) {
        for (var entryWordFromPage : frequencyOnPage.entrySet()) {

            String word = entryWordFromPage.getKey();
            int count = entryWordFromPage.getValue();

            //создаем объект по всем собранным данным
            PageEntry wordInfo = new PageEntry(pdf.getName(), pageNumber, count);

            //если в кеше уже есть такое слово
            if (cache.containsKey(word)) {
                cache.get(word).add(wordInfo);

            } else {
                cache.put(word, new ArrayList<>());
                cache.get(word).add(wordInfo);
            }
        }
    }

    @Override
    public List<PageEntry> search(String input) {
        //список для вывода итога
        List<PageEntry> resultList = new ArrayList<>();

        String[] words = input.split(" ");

        //проверяем каждое слово из ввода
        for (String word : words) {
            //учитываем слово, только если его нет в списке стоп слов

            if (cache.get(word) != null && !stopWords.contains(word)) {
                List<PageEntry> listFromCache = cache.get(word);

                //если еще нет записей, то просто все сохраняем
                if (resultList.isEmpty()) {
                    resultList.addAll(listFromCache);

                } else {
                    for (var cache : listFromCache) {
                        if (resultList.contains(cache)) {

                            var pageForChange = resultList.get(resultList.indexOf(cache));
                            pageForChange.setCount(pageForChange.getCount() + cache.getCount());

                            resultList.set(resultList.indexOf(cache), pageForChange);

                        } else {
                            resultList.add(cache);
                        }
                    }
                }
            }
        }
        resultList.sort(Comparator.reverseOrder());
        return resultList;
    }
}
