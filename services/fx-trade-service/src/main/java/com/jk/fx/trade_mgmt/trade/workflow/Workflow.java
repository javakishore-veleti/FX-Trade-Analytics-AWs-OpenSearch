package com.jk.fx.trade_mgmt.trade.workflow;

public interface Workflow<I, O> {
    O execute(I input);
}
