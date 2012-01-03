/*
 * Copyright 2011-2012 UnboundID Corp.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */

package com.unboundid.scim.sdk;

import org.apache.wink.client.ClientAuthenticationException;
import org.apache.wink.client.ClientRequest;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.handlers.ClientHandler;
import org.apache.wink.client.handlers.HandlerContext;
import org.apache.wink.common.http.HttpStatus;

import javax.xml.bind.DatatypeConverter;
import java.io.InputStream;

/**
 * This class provides HTTP Basic Authentication handling.
 */
public class HttpBasicAuthSecurityHandler implements ClientHandler {
  private volatile String username;
  private volatile String encodedCredentials;

  /**
   * Constructs a fully initialized Security handler.
   * @param username The Consumer username.
   * @param password The Consumer password.
   */
  public HttpBasicAuthSecurityHandler(final String username,
    final String password) {
    this.username = username;
    String encoded = DatatypeConverter.printBase64Binary(
        (username + ":" + password).getBytes());
    this.encodedCredentials = "Basic " + encoded;
  }

  /**
   * Attempts to authenticate a Consumer via Http Basic.
   *
   * @param request  The Client Resource request.
   * @param context The provided handler chain.
   * @return Client Response that may indicate success or failure.
   * @throws Exception Thrown if error handling authentication.
   */
  public ClientResponse handle(final ClientRequest request,
    final HandlerContext context) throws Exception {
    ClientResponse response = context.doChain(request);
    if (response.getStatusCode() == HttpStatus.UNAUTHORIZED.getCode()) {
      InputStream is = response.getEntity(InputStream.class);
      if(is != null) {
        // Throw away any entity content.
        is.close();
      }
      request.getHeaders().putSingle("Authorization", this.encodedCredentials);
      response = context.doChain(request);
      if (response.getStatusCode() == HttpStatus.UNAUTHORIZED.getCode()) {
        throw new ClientAuthenticationException(username);
      } else {
        // error presumably unrelated to authentication
        return response;
      }
    } else {
      return response;
    }
  }
}
