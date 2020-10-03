/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Armel Soro
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.rm3l.devfeed.persistence.utils

import org.rm3l.devfeed.common.articleparser.ArticleExtractor
import org.rm3l.devfeed.common.contract.Article
import org.rm3l.devfeed.common.screenshot.ArticleScreenshotExtractor
import org.rm3l.devfeed.persistence.ArticleUpdater
import org.rm3l.devfeed.persistence.DevFeedDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService

val logger: Logger = LoggerFactory.getLogger("handleArticles")

fun Collection<Article>?.handleAndPersistIfNeeded(dao: DevFeedDao,
                                                  executorService: ExecutorService,
                                                  articleScreenshotExtractor: ArticleScreenshotExtractor? = null,
                                                  articleParser: ArticleExtractor? = null,
                                                  synchronous: Boolean = true):
  Collection<CompletableFuture<Unit>> {
  val futures = this
    ?.asSequence()
    ?.map { article ->
      CompletableFuture.supplyAsync({
//        println("Inserting article: $article")
        article.tags = article.tags?.filterNotNull() ?: emptyList()
        if (!dao.existArticlesByUrl(article.url)) {
          val identifier = dao.insertArticle(article)
          dao.findArticleById(identifier)
        } else {
          dao.findArticleByUrl(article.url)
        }
      },
        executorService)
        .exceptionally {
          logger.warn("Could not insert article for $article", it)
          null
        }
    }?.mapNotNull { it.join() }
    ?.map { article ->
      if (articleScreenshotExtractor != null && article.screenshot == null) {
        CompletableFuture.supplyAsync({
//          println("Extracting screenshot for article, if any: $article")
          articleScreenshotExtractor.extractScreenshot(article)
          article
        },
          executorService)
      } else {
        CompletableFuture.completedFuture(article)
      }
    }
    ?.map { it.join() }
    ?.map { article ->
      if (articleParser != null && article.parsed == null) {
        CompletableFuture.supplyAsync({
//          println("Extract article data: $article")
          articleParser.extractArticleData(article)
          article
        }, executorService
        )
      } else {
        CompletableFuture.completedFuture(article)
      }
    }
    ?.map { it.join() }
    ?.map {
      CompletableFuture.supplyAsync({
//        println("Updating article as needed: $it")
        ArticleUpdater(dao, it).get()
      }, executorService)
        .exceptionally { exception ->
          logger.warn("Could not insert article for $it", exception)
        }
    }
    ?.toList() ?: listOf()

  if (synchronous) {
//    println("Handling synchronous call...")
    CompletableFuture.allOf(*futures.toTypedArray()).get() //Wait for all of them to finish
//    println("...done handling synchronous call!")
  }

  return futures
}
