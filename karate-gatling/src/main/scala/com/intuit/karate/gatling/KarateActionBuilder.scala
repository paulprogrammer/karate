package com.intuit.karate.gatling

import io.gatling.core.action.Action
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.structure.ScenarioContext

class KarateActionBuilder(requestName: String) extends ActionBuilder {
  override def build(ctx: ScenarioContext, next: Action): Action = {
    val karateComponents = ctx.protocolComponentsRegistry.components(KarateProtocol.KarateProtocolKey)
    new KarateAction(requestName, karateComponents.protocol, karateComponents.system, ctx.coreComponents.statsEngine, next)
  }
}
