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
package org.rm3l.devfeed.crawlers.common

import java.util.concurrent.Callable
import kotlin.reflect.KClass
import org.rm3l.devfeed.common.contract.Article

const val DEFAULT_THREAD_POOL_SIZE = 10

abstract class DevFeedCrawler : Callable<Collection<Article>> {

  companion object {

    @JvmStatic
    fun buildCliArgs(crawlerType: KClass<out DevFeedCrawler>, args: Array<String>): List<String> {
      if (args.isEmpty()) {
        throw IllegalArgumentException("Please specify a subcommand, e.g.: rdbms or mongo")
      }
      val argsList = args.toMutableList()
      val crawlerOption = "--crawler=${crawlerType.qualifiedName}"
      if (args.size == 1) {
        argsList.add(crawlerOption)
      } else {
        argsList.add(1, crawlerOption)
      }
      return argsList
    }
  }
}
