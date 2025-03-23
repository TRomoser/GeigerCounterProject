/*
 * @author Taylor Romoser
 * @date 3/22/25
 *  To determine the most likely date of the camping trip, I wrote 
 *  a program that:
 * 1. Read all the radiation samples from the log file.
 * 2. Found the maximum counts per minute recorded.
 * 3. Collected all samples within 5 counts of that maximum.
 * 4. Grouped these high readings by date.
 * 5. Identified the date with the most high-CPM readings.
 * 
 * I figured that a camping trip at higher elevation (4900 ft) 
 * would not result in a single high reading, but a full day of elevated 
 * radiation levels. By grouping and counting high readings per day,
 * I was able to identify the most likely date you were at a higher elevation.
 * 
 * Based on this method, the most likely camping trip date was: June 1, 2019
 * But based on the surrounding data you likely started the trip on 
 * May 31, 2019 and ended on June 2, 2019
 * 
 * Challenges/roadblocks:
 * - I initially was challenged by the first lines of the source file, I needed
 *  to skip the ones that were shorter than 3 entries
 * - At one point I tried chaining methods like
 *  sample.getCountsPerMinute().getDateTime(), then realized I needed to
 *  isolate them in variables then do the parsing
 * - Initially I was using DateTimeFormatter.ofPattern("M/d/yyyy HH:mm"),
 *  but I needed to use "M/d/yyyy H:mm" to match data
 * - I initially made the table line by line but I had fun learning
 *  how to format properly
 * - I originally tried using Duration to find a long streak of high readings,
 *  but realized I just needed to find the day with the most elevated samples.
 * - A malformed date ("2019-05-32 00:11") caused parsing errors. I handled it 
 *  with a try/catch block to skip the bad line. If corrected, it would add one 
 *  more high reading to June 1, 2019 (bringing the total to 22).
 */

import java.io.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;

public class Main {
  public static void main(String[] args) {
    String FILENAME = "7_14_2019.txt";
    ArrayList<RadiationSample> samples = new ArrayList<>();
    int maxCountPerMin = 0;
    
    // Read text file and add to sample list, 
    // while also finding the maxCount
    try(BufferedReader reader = new BufferedReader(new FileReader(FILENAME))) {
      String line;
      while((line = reader.readLine()) != null) {
        String[] newLine = line.split(",");
        // Skip first lines without count data
        if(newLine.length < 3) continue;

        try {
          RadiationSample sample = new RadiationSample(newLine[0], Integer.parseInt(newLine[2]));
          if(sample.getCountsPerMinute() > maxCountPerMin) {
            maxCountPerMin = sample.getCountsPerMinute();
          }
          samples.add(sample);
        } catch(NumberFormatException e) {
          continue;
        }
      }
    } catch(IOException e) {
      System.err.println("Error reading file: " + e.getMessage());
    }
    
    // Add samples with count within 5 of maxCount to new list
    ArrayList<RadiationSample> maxCountSamples = new ArrayList<>();
    for(RadiationSample sample: samples) {
      if(sample.getCountsPerMinute() >= maxCountPerMin - 5) {
        maxCountSamples.add(sample);
      }
    }

    // Print table of results
    printSamplesInTable("Radiation samples with CPM >= (max - 5)", maxCountSamples);

    // Create hash map with dates as keys and samples as values
    Map<LocalDate, List<RadiationSample>> groupedByDate = new HashMap<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy H:mm");
    for(RadiationSample sample: maxCountSamples) {
      // Parse date from dateTime string
      // try/catch to skip a line in the source 
      // with date 2019-05-32 00:11 (invalid)
      try {
        LocalDateTime dateTime = LocalDateTime.parse(sample.getDateTime(), formatter);
        LocalDate date = dateTime.toLocalDate();
        groupedByDate.putIfAbsent(date, new ArrayList<>());
        groupedByDate.get(date).add(sample);
    } catch (Exception e) {
        System.err.println("Skipping malformed date: " + sample.getDateTime());
    }
    }

    // Determine which date is most likely the camping date
    LocalDate campDate = null;
    int mostReadings = 0;
    for(Map.Entry<LocalDate, List<RadiationSample>> entry: groupedByDate.entrySet()) {
      if(entry.getValue().size() > mostReadings) {
        mostReadings = entry.getValue().size();
        campDate = entry.getKey();
      }
    }

    // Print camping date and table of samples that day
    System.out.println("Most Likely Camping trip date: " + campDate + ", number of high counts that day : " + mostReadings);
    printSamplesInTable("Samples that day", groupedByDate.get(campDate));
  }

  public static void printSamplesInTable(String title, List<RadiationSample> samples) {
    int totalWidth = 30;
    int padding = (totalWidth - title.length()) / 2;

    System.out.println("-".repeat(totalWidth));
    System.out.printf("%" + (padding + title.length()) + "s\n", title);
    System.out.println("-".repeat(totalWidth));
    for (RadiationSample sample : samples) {
        System.out.printf("| %-20s | %3d |\n", sample.getDateTime(), sample.getCountsPerMinute());
    }
    System.out.println("-".repeat(totalWidth));
  }
}