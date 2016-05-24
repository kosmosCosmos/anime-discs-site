package com.animediscs.spider;

import com.animediscs.dao.Dao;
import com.animediscs.model.*;
import com.animediscs.runner.SpiderService;
import com.animediscs.runner.task.RankSpiderTask;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.animediscs.model.Disc.*;
import static com.animediscs.util.Helper.nullSafeGet;
import static com.animediscs.util.Parser.parseNumber;

@Service
public class AmazonRankSpider {

    private Logger logger = LogManager.getLogger(AmazonRankSpider.class);

    private Dao dao;

    @Autowired
    public void setDao(Dao dao) {
        this.dao = dao;
    }

    public void doUpdateHot(SpiderService service, int level) {
        if (service.isBusy(level)) {
            logger.printf(Level.INFO, "抓取服务忙, 暂停添加任务");
            return;
        }
        dao.execute(session -> {
            DiscList discList = (DiscList) findLatestSakura(session)
                    .setFirstResult(1)
                    .setMaxResults(1)
                    .uniqueResult();
            List<Disc> discs = discList.getDiscs().stream()
                    .sorted(sortBySakura()).limit(15)
                    .collect(Collectors.toList());
            AtomicBoolean needUpdate = new AtomicBoolean(false);
            AtomicInteger count = new AtomicInteger(discs.size());
            infoUpdateStart("Amazon(Hot)", discList.getTitle(), count);
            discs.forEach(disc -> {
                service.addTask(level, new RankSpiderTask(disc.getAsin(), needUpdate(disc), document -> {
                    if (updateRank(disc, document, count, Level.INFO)) {
                        needUpdate.set(true);
                    }
                    if (count.get() == 0) {
                        infoUpdateFinish("Amazon(Hot)", discList.getTitle());
                        if (needUpdate.get()) {
                            logger.info("发现排名有变化, 准备更新全部排名数据");
                            doUpdateAll(service, level + 1);
                        }
                    }
                }));
            });
        });
    }

    public void doUpdateAll(SpiderService service, int level) {
        if (service.isBusy(level)) {
            logger.printf(Level.INFO, "抓取服务忙, 暂停添加任务");
            return;
        }
        Set<Disc> discs = new LinkedHashSet<>();
        dao.execute(session -> {
            Set<Disc> later = new LinkedHashSet<>();
            findLatestSakura(session).list().forEach(o -> {
                DiscList discList = (DiscList) o;
                discList.getDiscs().stream()
                        .sorted(sortBySakura())
                        .limit(40)
                        .forEach(discs::add);
                discList.getDiscs().stream()
                        .sorted(sortBySakura())
                        .skip(40)
                        .forEach(later::add);

            });
            Stream.of("kabaneri", "macross").forEach(name -> {
                dao.lookup(DiscList.class, "name", name).getDiscs()
                        .stream().sorted(sortByAmazon()).forEach(discs::add);
            });
            findNotSakura(session).list().forEach(o -> {
                DiscList discList = (DiscList) o;
                discList.getDiscs().stream()
                        .sorted(sortByAmazon())
                        .forEach(later::add);
            });
            later.forEach(discs::add);
        });
        dao.findAll(Disc.class).stream()
                .sorted(sortByAmazon())
                .forEach(discs::add);
        AtomicBoolean needUpdate = new AtomicBoolean(false);
        AtomicInteger count = new AtomicInteger(discs.size());
        infoUpdateStart("Amazon(All)", "所有碟片", count);
        discs.forEach(disc -> {
            service.addTask(level, new RankSpiderTask(disc.getAsin(), needUpdate(disc), document -> {
                if (updateRank(disc, document, count, Level.INFO)) {
                    needUpdate.set(true);
                }
                if (count.get() == 0) {
                    infoUpdateFinish("Amazon(All)", "所有碟片");
                    if (needUpdate.get()) {
                        logger.info("发现排名有变化, 准备再次更新排名数据");
                        doUpdateAll(service, level);
                    }
                }
            }));
        });

    }

    private Criteria findLatestSakura(Session session) {
        Date yesterday = DateUtils.addDays(new Date(), -1);
        return session.createCriteria(DiscList.class)
                .add(Restrictions.eq("sakura", true))
                .add(Restrictions.gt("date", yesterday))
                .addOrder(Order.desc("name"));
    }

    private Criteria findNotSakura(Session session) {
        return session.createCriteria(DiscList.class)
                .add(Restrictions.eq("sakura", false))
                .addOrder(Order.desc("name"));
    }

    private Supplier<Boolean> needUpdate(Disc disc) {
        return () -> needUpdate(nullSafeGet(disc.getRank(), DiscRank::getPadt1));
    }

    private boolean updateRank(Disc disc, Document document, AtomicInteger count, Level level) {
        if (document != null) {
            Node rankNode = document.getElementsByTagName("SalesRank").item(0);
            if (rankNode != null) {
                if (updateRank(disc, rankNode)) {
                    loggerRankChange(level, disc, count);
                    return true;
                } else {
                    loggerRankUnChange(level, disc, count);
                }
            } else {
                loggerNoRank(level, disc, count);
            }
        } else {
            loggerSkipUpdate(level, disc, count);
        }
        return false;
    }

    private boolean updateRank(Disc disc, Node rankNode) {
        boolean rankChanged = false;
        DiscRank discRank = getDiscRank(disc);
        if (needUpdate(discRank.getPadt1())) {
            discRank.setPark(parseNumber(rankNode.getTextContent()));
            discRank.setPadt(new Date());
            if (discRank.getPark() != discRank.getPark1()) {
                pushRank(discRank);
                saveRank(discRank);
                rankChanged = true;
            }
            dao.saveOrUpdate(discRank);
        }
        return rankChanged;
    }

    private boolean needUpdate(Date date) {
        Date twentyMintue = DateUtils.addMinutes(new Date(), -20);
        return date == null || date.compareTo(twentyMintue) < 0;
    }

    private void infoUpdateStart(String name, String title, AtomicInteger count) {
        logger.printf(Level.INFO, "开始更新%s排名数据(%s), 总共%d个", name, title, count.get());
    }

    private void infoUpdateFinish(String name, String title) {
        logger.printf(Level.INFO, "成功更新%s排名数据(%s)", name, title);
    }

    private void loggerSkipUpdate(Level level, Disc disc, AtomicInteger count) {
        logger.printf(level, "排名数据不需更新, ASIN=%s, Rank=%d, Title=%s, 还剩%d个未更新",
                disc.getAsin(), disc.getRank().getPark1(), disc.getTitle(), count.decrementAndGet());
    }

    private void loggerRankChange(Level level, Disc disc, AtomicInteger count) {
        logger.printf(level, "排名数据发生变化, ASIN=%s, Rank=%d->%d, Title=%s, 还剩%d个未更新",
                disc.getAsin(), disc.getRank().getPark2(), disc.getRank().getPark1(), disc.getTitle(), count.decrementAndGet());
    }

    private void loggerRankUnChange(Level level, Disc disc, AtomicInteger count) {
        logger.printf(level, "排名数据保持不变, ASIN=%s, Rank=%d, Title=%s, 还剩%d个未更新",
                disc.getAsin(), disc.getRank().getPark1(), disc.getTitle(), count.decrementAndGet());
    }

    private void loggerNoRank(Level level, Disc disc, AtomicInteger count) {
        logger.printf(level, "未找到排名数据, ASIN=%s, Title=%s, 还剩%d个未更新",
                disc.getAsin(), disc.getTitle(), count.decrementAndGet());
    }

    private DiscRank getDiscRank(Disc disc) {
        DiscRank rank = disc.getRank();
        if (rank == null) {
            rank = new DiscRank();
            rank.setDisc(disc);
            disc.setRank(rank);
        } else {
            dao.refresh(rank);
        }
        return rank;
    }

    private void pushRank(DiscRank rank) {
        rank.setPadt5(rank.getPadt4());
        rank.setPadt4(rank.getPadt3());
        rank.setPadt3(rank.getPadt2());
        rank.setPadt2(rank.getPadt1());
        rank.setPadt1(rank.getPadt());
        rank.setPark5(rank.getPark4());
        rank.setPark4(rank.getPark3());
        rank.setPark3(rank.getPark2());
        rank.setPark2(rank.getPark1());
        rank.setPark1(rank.getPark());
    }

    private void saveRank(DiscRank rank) {
        DiscRecord record = new DiscRecord();
        record.setDisc(rank.getDisc());
        record.setDate(rank.getPadt());
        record.setRank(rank.getPark());
        dao.save(record);
    }

}