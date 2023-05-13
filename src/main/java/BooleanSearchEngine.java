import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BooleanSearchEngine implements SearchEngine {
    //в этой мапе будем хранить результаты сканирования всех наших файлов, в качестве ключа - слово для поиска
    private final Map<String, List<PageEntry>> cache;

    public BooleanSearchEngine(File pdfsDir) throws IOException {

        cache = new HashMap<>();

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
    public List<PageEntry> search(String word) {
        //поиск по слову в кеше
        List<PageEntry> list = cache.getOrDefault(word, null);
        list.sort(Comparator.reverseOrder());
        return list;
    }
}
