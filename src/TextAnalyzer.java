import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class TextAnalyzer {

    private static final int TEXT_COUNT = 10_000;
    private static final int QUEUE_CAPACITY = 100;

    private static final BlockingQueue<String> queueA = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private static final BlockingQueue<String> queueB = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private static final BlockingQueue<String> queueC = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    private static final AtomicInteger maxCountA = new AtomicInteger();
    private static final AtomicInteger maxCountB = new AtomicInteger();
    private static final AtomicInteger maxCountC = new AtomicInteger();

    public static void main(String[] args) throws InterruptedException {
        Thread generatorThread = new Thread(() -> {
            for (int i = 0; i < TEXT_COUNT; i++) {
                String text = generateText("abc", 100_000);
                try {
                    queueA.put(text);
                    queueB.put(text);
                    queueC.put(text);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            // Помещаем стоп-сигналы после генерации всех текстов
            try {
                queueA.put("STOP");
                queueB.put("STOP");
                queueC.put("STOP");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread analyzerThreadA = createAnalyzerThread(queueA, 'a', maxCountA);
        Thread analyzerThreadB = createAnalyzerThread(queueB, 'b', maxCountB);
        Thread analyzerThreadC = createAnalyzerThread(queueC, 'c', maxCountC);

        generatorThread.start();
        analyzerThreadA.start();
        analyzerThreadB.start();
        analyzerThreadC.start();

        generatorThread.join();
        analyzerThreadA.join();
        analyzerThreadB.join();
        analyzerThreadC.join();

        System.out.println("Максимальное количество 'a': " + maxCountA.get());
        System.out.println("Максимальное количество 'b': " + maxCountB.get());
        System.out.println("Максимальное количество 'c': " + maxCountC.get());
    }

    public static String generateText(String letters, int length) {
        Random random = new Random();
        StringBuilder text = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            text.append(letters.charAt(random.nextInt(letters.length())));
        }
        return text.toString();
    }

    private static Thread createAnalyzerThread(BlockingQueue<String> queue, char ch, AtomicInteger maxCount) {
        return new Thread(() -> {
            try {
                while (true) {
                    String text = queue.take();
                    if (text.equals("STOP")) break; // Завершение потока по специальному сигналу
                    int count = (int) text.chars().filter(c -> c == ch).count();
                    maxCount.updateAndGet(x -> Math.max(x, count));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
