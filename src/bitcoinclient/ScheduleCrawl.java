/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bitcoinclient;

import java.io.IOException;

public class ScheduleCrawl {

    public static void main(String[] args) throws InterruptedException {
        // TODO Auto-generated method stub
        long start;
        start = System.currentTimeMillis();

        while (true) {
            long nextCrawl = System.currentTimeMillis() + 3600 * 1000 * 3;

            try {
                NetworkCrawler networkCrawler;
                networkCrawler = new NetworkCrawler();
            } catch (InterruptedException | IOException e) {
            }
            // TODO Auto-generated catch block

            System.out.println("Waiting for next crawl time");
            while (System.currentTimeMillis() < nextCrawl) {
                Thread.sleep(5000);
            }

        }

    }

}
