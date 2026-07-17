package com.ml.tblandroidtxt;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class Version320Test {
    @Test public void providerCapabilitiesAreExplicitAndCustomIsSafe(){AppSettings s=new AppSettings();s.provider="openrouter";assertTrue(ProviderCapabilities.forSettings(s).cachedTokenReporting);s.provider="custom";s.baseUrl="https://example.test/v1/chat/completions";assertFalse(ProviderCapabilities.forSettings(s).promptCaching);}
    @Test public void cachedPricingDoesNotDoubleChargeInput(){ModelCatalog.ModelInfo m=new ModelCatalog.ModelInfo("m",1,4,0.25,1000,"p","test");double cost=ModelCatalog.usageCost(m,1000,400,500);assertEquals(0.0027,cost,0.0000001);}
    @Test public void sampleSelectionIsStableAndBounded(){List<TranslationRepository.ChunkRow> rows=new ArrayList<>();for(int i=0;i<10;i++){TranslationRepository.ChunkRow r=new TranslationRepository.ChunkRow();r.index=i;r.source="s"+i;rows.add(r);}List<TranslationRepository.ChunkRow> selected=LiveBenchmarkRunner.selectSamples(rows);assertEquals(3,selected.size());assertEquals(0,selected.get(0).index);assertEquals(5,selected.get(1).index);assertEquals(9,selected.get(2).index);}
    @Test public void worstCaseBudgetUsesSameOutputPolicyForBothPipelines(){AppSettings s=new AppSettings();s.maxOutputTokens=1000;ModelCatalog.ModelInfo m=new ModelCatalog.ModelInfo(s.model,1,4,10000,s.provider);TranslationRepository.ChunkRow r=new TranslationRepository.ChunkRow();r.index=0;r.source="short source";double cost=LiveBenchmarkRunner.estimateWorst(java.util.Collections.singletonList(r),s,m,1000);assertTrue(cost>0.008);}
}
