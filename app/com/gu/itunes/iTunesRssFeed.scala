package com.gu.itunes

import com.gu.contentapi.client.model.v1.ItemResponse
import com.gu.contentapi.client.model.v1._
import org.joda.time.DateTime
import org.scalactic.{ Bad, Good, Or }

import scala.xml.Node

object iTunesRssFeed {

  def apply(resp: ItemResponse): Node Or String = resp.tag match {
    case Some(t) => toXml(t, resp.results.getOrElse(Nil).toList)
    case None => Bad("No tag found")
  }

  def toXml(tag: Tag, contents: List[Content]): Node Or String = {

    tag.podcast match {
      case Some(podcast) => Good {
        <rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" version="2.0">
          <channel>
            <title>{ tag.webTitle }</title>
            <link>{ tag.webUrl }</link>
            <description>{ tag.description.getOrElse("") }</description>
            <language>en-gb</language>
            <copyright>{ podcast.copyright }</copyright>
            <lastBuildDate>
              { DateSupport.toRssTimeFormat(DateTime.now) }
            </lastBuildDate>
            <ttl>15</ttl>
            <itunes:owner>
              <itunes:email>userhelp@theguardian.com</itunes:email>
              <itunes:name>theguardian.com</itunes:name>
            </itunes:owner>
            <itunes:image href={ podcast.image.getOrElse("") }/>
            <itunes:author>theguardian.com</itunes:author>
            {
              if (podcast.explicit)
                <itunes:explicit>yes</itunes:explicit>
            }
            <itunes:keywords/>
            <itunes:summary>{ tag.description.getOrElse("") }</itunes:summary>
            <image>
              <title>{ tag.webTitle }</title>
              <url>http://static.guim.co.uk/sitecrumbs/Guardian.gif</url>
              <link>http://www.theguardian.com</link>
            </image>
            {
              for (category <- podcast.categories.getOrElse(Nil)) yield new CategoryRss(category).toXml
            }
            {
              for (p <- contents.filter(c => hasEpisode(c))) yield new iTunesRssItem(p, tag.id).toXml
            }
          </channel>
        </rss>
      }
      case None => Bad {
        "No podcast found"
      }
    }
  }

  def hasEpisode(content: Content): Boolean = {
    /* should contain at least one audio asset */
    content.elements.exists(elements => elements.flatMap(e => e.assets).exists(_.`type` == AssetType.Audio))
  }
}

class CategoryRss(val category: PodcastCategory) {
  def toXml: Node = {
    <itunes:category text={ category.main }>
      {
        category.sub match {
          case Some(s) => <itunes:category text={ s }/>
          case None =>
        }
      }
    </itunes:category>
  }

}
