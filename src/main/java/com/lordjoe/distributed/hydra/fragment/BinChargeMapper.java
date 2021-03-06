package com.lordjoe.distributed.hydra.fragment;

import com.lordjoe.distributed.*;
import com.lordjoe.distributed.hydra.scoring.*;
import org.apache.spark.api.java.*;
import org.systemsbiology.xtandem.*;
import org.systemsbiology.xtandem.peptide.*;
import scala.*;

import java.io.Serializable;
import java.util.*;

/**
 * com.lordjoe.distributed.hydra.fragment.BinChargeMapper
 * User: Steve
 * Date: 10/31/2014
 */
public class BinChargeMapper implements Serializable {

    public static final double binSize = BinChargeKey.QUANTIZATION;
    public static final double examineWidth = 1.2;


    private final XTandemMain application;
    //   private final Scorer scorer;
//    private final IScoringAlgorithm algorithm;


    public BinChargeMapper(SparkMapReduceScoringHandler pHandler) {
        this(pHandler.getApplication());
    }

    public BinChargeMapper(XTandemMain app) {
        application = app;
        //      scorer = application.getScoreRunner();
        //      algorithm = application.getAlgorithms()[0];
    }

    public JavaPairRDD<BinChargeKey, IMeasuredSpectrum> mapMeasuredSpectrumToKeys(JavaRDD<IMeasuredSpectrum> inp) {
        return inp.flatMapToPair(new mapMeasuredSpectraToBins());
    }

    public JavaPairRDD<BinChargeKey, Tuple2<BinChargeKey, IMeasuredSpectrum>> mapMeasuredSpectrumToKeySpectrumPair(JavaRDD<IMeasuredSpectrum> inp) {
        inp = SparkUtilities.repartitionIfNeeded(inp);
        return inp.flatMapToPair(new mapMeasuredSpectraToBinTuples());
    }


    public JavaPairRDD<BinChargeKey, IPolypeptide> mapFragmentsToKeys(JavaRDD<IPolypeptide> inp) {
        return inp.flatMapToPair(new mapPolypeptidesToBins());
    }

    public BinChargeKey[] keysFromChargeMz(int charge, double mz) {
        List<BinChargeKey> holder = new ArrayList<BinChargeKey>();
        double startMZ = mz - examineWidth;
        int start = BinChargeKey.mzAsInt(startMZ);
        while (BinChargeKey.intToMz(start) < mz + examineWidth) {
            holder.add(new BinChargeKey(charge, BinChargeKey.intToMz(start++)));
        }
//        double mzStart = ((int) (0.5 + ((mz - examineWidth) / binSize))) * binSize;
//        for (int i = 0; i < examineWidth / binSize; i++) {
//            double quantizedMz = (mzStart + i) * binSize;
//            holder.add(new BinChargeKey(charge, quantizedMz)); // todo add meighbors
//
//        }


        BinChargeKey[] ret = new BinChargeKey[holder.size()];
        holder.toArray(ret);
        return ret;
    }

    /**
     * create one key from change and MZ
     *
     * @param charge
     * @param mz
     * @return
     */
    static public BinChargeKey oneKeyFromChargeMz(int charge, double mz) {
        List<BinChargeKey> holder = new ArrayList<BinChargeKey>();
        double mzStart = ((int) (0.5 + ((mz) / binSize))) * binSize;
        double quantizedMz = mzStart ;
        BinChargeKey ret = new BinChargeKey(charge, quantizedMz);
        double mzx = ret.getMz();
        if(Math.abs(mz - mzx) > binSize)
            throw new IllegalStateException("bad bin key");
        return ret;
    }

    /**
     * peptides are only mapped once whereas spectra map to multiple  bins
     */
    private class mapPolypeptidesToBins extends AbstractLoggingPairFlatMapFunction<IPolypeptide, BinChargeKey, IPolypeptide> {
        @Override
        public Iterable<Tuple2<BinChargeKey, IPolypeptide>> doCall(final IPolypeptide pp) throws Exception {
            double matchingMass = pp.getMatchingMass();
            List<Tuple2<BinChargeKey, IPolypeptide>> holder = new ArrayList<Tuple2<BinChargeKey, IPolypeptide>>();
            for (int charge = 1; charge < 4; charge++) {
                BinChargeKey key = oneKeyFromChargeMz(charge, matchingMass / charge );
                holder.add(new Tuple2<BinChargeKey, IPolypeptide>(key, pp));
            }
            if (holder.isEmpty())
                throw new IllegalStateException("problem"); // ToDo change

            return holder;
        }
    }

    private class mapMeasuredSpectraToBins extends AbstractLoggingPairFlatMapFunction<IMeasuredSpectrum, BinChargeKey, IMeasuredSpectrum> {
        @Override
        public Iterable<Tuple2<BinChargeKey, IMeasuredSpectrum>> doCall(final IMeasuredSpectrum spec) throws Exception {
            double matchingMass = spec.getPrecursorMass();   // todo decide whether mass or mz is better
            double specMZ = spec.getPrecursorMassChargeRatio();
            int charge = spec.getPrecursorCharge();
            List<Tuple2<BinChargeKey, IMeasuredSpectrum>> holder = new ArrayList<Tuple2<BinChargeKey, IMeasuredSpectrum>>();
            BinChargeKey[] keys = keysFromChargeMz(charge, specMZ);
            for (int i = 0; i < keys.length; i++) {
                BinChargeKey key = keys[i];
                holder.add(new Tuple2<BinChargeKey, IMeasuredSpectrum>(key, spec));
            }
            if (holder.isEmpty())
                throw new IllegalStateException("problem"); // ToDo change
            return holder;
        }
    }

    private class mapMeasuredSpectraToBinTuples extends AbstractLoggingPairFlatMapFunction<IMeasuredSpectrum, BinChargeKey, Tuple2<BinChargeKey, IMeasuredSpectrum>> {
        @Override
        public Iterable<Tuple2<BinChargeKey, Tuple2<BinChargeKey, IMeasuredSpectrum>>> doCall(final IMeasuredSpectrum spec) throws Exception {
            double matchingMass = spec.getPrecursorMass();
            int charge = spec.getPrecursorCharge();
            List<Tuple2<BinChargeKey, Tuple2<BinChargeKey, IMeasuredSpectrum>>> holder = new ArrayList<Tuple2<BinChargeKey, Tuple2<BinChargeKey, IMeasuredSpectrum>>>();
            BinChargeKey[] keys = keysFromChargeMz(charge, matchingMass);
            for (int i = 0; i < keys.length; i++) {
                BinChargeKey key = keys[i];
                holder.add(new Tuple2<BinChargeKey, Tuple2<BinChargeKey, IMeasuredSpectrum>>(key, new Tuple2<BinChargeKey, IMeasuredSpectrum>(key, spec)));
            }
            if (holder.isEmpty())
                throw new IllegalStateException("problem"); // ToDo change
            return holder;
        }
    }
}
