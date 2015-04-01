/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tonicsystems.jarjar.transform.jar;

import com.tonicsystems.jarjar.transform.EntryStruct;
import java.io.IOException;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public abstract class AbstractFilterJarProcessor implements JarProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractFilterJarProcessor.class);

    protected abstract boolean isFiltered(@Nonnull String name);

    @Override
    public Result scan(EntryStruct struct) throws IOException {
        if (isFiltered(struct.name)) {
            LOG.debug("{}.scan discarded {}", getClass().getSimpleName(), struct.name);
            return Result.DISCARD;
        }
        return Result.KEEP;
    }

    @Override
    public Result process(EntryStruct struct) throws IOException {
        if (isFiltered(struct.name)) {
            LOG.debug("{}.process discarded {}", getClass().getSimpleName(), struct.name);
            return Result.DISCARD;
        }
        return Result.KEEP;
    }
}
