/*
 * Copyright © 2017 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.gateway.handlers;

import co.cask.cdap.api.annotation.Beta;
import co.cask.cdap.common.conf.Constants;
import co.cask.cdap.metadata.MetadataService;
import co.cask.http.AbstractHttpHandler;
import co.cask.http.HttpHandler;
import co.cask.http.HttpResponder;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * {@link HttpHandler} for getting info about upgrade progress, if applicable.
 */
@Singleton
@Beta
@Path(Constants.Gateway.API_VERSION_3 + "/system/upgrade")
public class UpgradeHttpHandler extends AbstractHttpHandler {

  private static final Gson GSON = new Gson();
  private Map<String, Boolean> upgradeStatus;
  private MetadataService metadataService;

  @Inject
  UpgradeHttpHandler(MetadataService metadataService) {
    this.upgradeStatus = new HashMap<>();
    this.metadataService = metadataService;
  }

  @GET
  @Path("/status")
  public void getUpgradeStatus(HttpRequest request, HttpResponder responder) throws Exception {
    upgradeStatus.put("metadata", metadataService.isMigrationInProcess());
    responder.sendJson(HttpResponseStatus.OK, GSON.toJson(upgradeStatus));
  }
}
