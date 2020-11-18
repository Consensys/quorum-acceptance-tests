/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.quorum.gauge.common;

/**
 * Named values used to talk about specific nodes in spec files.
 *
 * @deprecated
 * <p> Use {@link QuorumNetworkProperty.Node} instead.
 */
@Deprecated
public enum QuorumNode {
    Node1, Node1_PS1, Node1_PS2, Node1_PS3,
    Node2, Node2_PS1, Node2_PS2, Node2_PS3,
    Node3, Node3_PS1, Node3_PS2, Node3_PS3,
    Node4, Node4_PS1, Node4_PS2, Node4_PS3,
    Node5, Node5_PS1, Node5_PS2, Node5_PS3,
    Node6, Node6_PS1, Node6_PS2, Node6_PS3,
    Node7, Node7_PS1, Node7_PS2, Node7_PS3,
    Node8, Node8_PS1, Node8_PS2, Node8_PS3,
    Node9, Node9_PS1, Node9_PS2, Node9_PS3,
    Node10, Node10_PS1, Node10_PS2, Node10_PS3,
}
