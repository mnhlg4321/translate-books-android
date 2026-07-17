package com.ml.tblandroidtxt;

import org.junit.Test;

import java.util.List;
import java.util.Collections;

import static org.junit.Assert.*;

public class Hotfix441Test {
    private static AppSettings settings() {
        AppSettings s=new AppSettings();
        s.chunkMode="chars";
        s.maxCharsPerChunk=1800;
        s.maxTokensPerChunk=1000;
        s.contextChars=0;
        s.contextOverlapEnabled=false;
        return AppValidator.normalize(s);
    }

    @Test(timeout=5000) public void largeJapaneseInputUsesLinearCancellableChunkScan() {
        String paragraph="第一章。これは大きな日本語テキストの性能試験です。会話と段落の境界を保ちながら正確に分割します。\n";
        StringBuilder source=new StringBuilder(600_000);
        while(source.length()<600_000)source.append(paragraph);
        List<Chunk> chunks=Chunker.chunkText(source.toString(),settings());
        assertTrue(chunks.size()>100);
        assertTrue(Chunker.verifyCoverage(Chunker.normalizeSource(source.toString()),chunks).valid);
    }

    @Test public void preparedEstimateUsesTheExactProductionChunks() {
        String source="一。二。三。四。五。六。七。八。九。十。".repeat(500);
        AppSettings s=settings();
        List<Chunk> plan=Chunker.chunkText(source,s);
        CostEstimator.Estimate estimate=CostEstimator.estimatePreparedChunks(plan,s);
        assertEquals(plan.size(),estimate.chunks);
        assertTrue(estimate.totalTokensHigh>=estimate.totalTokensLow);
        assertTrue(estimate.totalTokensHigh>0);
    }

    @Test public void releaseMetadataIs48Code47() {
        assertEquals("4.8",AppBuildInfo.VERSION_NAME);
        assertEquals(47,AppBuildInfo.VERSION_CODE);
    }

    @Test public void promptOnlyChangeReusesConstructionButReestimates() {
        AppSettings first=settings(),second=first.copy();second.glossaryText="勇者 = anh hùng";
        String construction=PreparationCoordinator.constructionKey(first);
        assertEquals(construction,PreparationCoordinator.constructionKey(second));
        assertNotEquals(PreparationCoordinator.estimateKey(first,construction),PreparationCoordinator.estimateKey(second,construction));
        second.maxCharsPerChunk++;
        assertNotEquals(construction,PreparationCoordinator.constructionKey(second));
    }

    @Test public void pricingLoadingOrUnavailableDoesNotBlockTokensOrChunks() {
        AppSettings s=settings();s.provider="unlisted";s.model="unlisted/model";List<Chunk> plan=Chunker.chunkText("価格から独立した準備です。".repeat(100),s);
        ModelCatalog.installForTests(Collections.emptyList(),ModelCatalog.LoadingState.LOADING);
        CostEstimator.Estimate loading=CostEstimator.estimatePreparedChunks(plan,s);assertEquals(plan.size(),loading.chunks);assertTrue(loading.totalTokensHigh>0);assertFalse(loading.pricingAvailable);
        ModelCatalog.installForTests(Collections.emptyList(),ModelCatalog.LoadingState.ERROR);
        CostEstimator.Estimate unavailable=CostEstimator.estimatePreparedChunks(plan,s);assertEquals(plan.size(),unavailable.chunks);assertTrue(unavailable.totalTokensHigh>0);assertFalse(unavailable.pricingAvailable);
        ModelCatalog.installForTests(Collections.emptyList(),ModelCatalog.LoadingState.READY);
    }

    @Test public void interruptedPreparationStopsBeforeWorkContinues() {
        Thread.currentThread().interrupt();
        try{Chunker.chunkText("停止可能な準備です。".repeat(1000),settings());fail("interrupted preparation must stop");}
        catch(Chunker.PreparationCancelledException expected){assertTrue(expected.getMessage().contains("cancelled"));}
        finally{Thread.interrupted();}
    }
}
