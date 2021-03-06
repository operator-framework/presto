/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestosql.plugin.mongodb;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mongodb.MongoClient;
import io.airlift.log.Logger;
import io.airlift.log.Logging;
import io.airlift.tpch.TpchTable;
import io.prestosql.Session;
import io.prestosql.plugin.tpch.TpchPlugin;
import io.prestosql.testing.DistributedQueryRunner;

import java.util.Map;

import static io.airlift.testing.Closeables.closeAllSuppress;
import static io.prestosql.plugin.tpch.TpchMetadata.TINY_SCHEMA_NAME;
import static io.prestosql.testing.QueryAssertions.copyTpchTables;
import static io.prestosql.testing.TestingSession.testSessionBuilder;

public final class MongoQueryRunner
{
    private static final String TPCH_SCHEMA = "tpch";

    private MongoQueryRunner() {}

    public static DistributedQueryRunner createMongoQueryRunner(MongoServer server, TpchTable<?>... tables)
            throws Exception
    {
        return createMongoQueryRunner(server, ImmutableList.copyOf(tables));
    }

    public static DistributedQueryRunner createMongoQueryRunner(MongoServer server, Iterable<TpchTable<?>> tables)
            throws Exception
    {
        DistributedQueryRunner queryRunner = null;
        try {
            queryRunner = DistributedQueryRunner.builder(createSession()).build();

            queryRunner.installPlugin(new TpchPlugin());
            queryRunner.createCatalog("tpch", "tpch");

            Map<String, String> properties = ImmutableMap.of(
                    "mongodb.seeds", server.getAddress().toString(),
                    "mongodb.socket-keep-alive", "true");

            queryRunner.installPlugin(new MongoPlugin());
            queryRunner.createCatalog("mongodb", "mongodb", properties);

            copyTpchTables(queryRunner, "tpch", TINY_SCHEMA_NAME, createSession(), tables);
            return queryRunner;
        }
        catch (Throwable e) {
            closeAllSuppress(e, queryRunner);
            throw e;
        }
    }

    public static Session createSession()
    {
        return testSessionBuilder()
                .setCatalog("mongodb")
                .setSchema(TPCH_SCHEMA)
                .build();
    }

    public static MongoClient createMongoClient(MongoServer server)
    {
        return new MongoClient(server.getAddress().getHost(), server.getAddress().getPort());
    }

    public static void main(String[] args)
            throws Exception
    {
        Logging.initialize();
        DistributedQueryRunner queryRunner = createMongoQueryRunner(new MongoServer(), TpchTable.getTables());
        Thread.sleep(10);
        Logger log = Logger.get(MongoQueryRunner.class);
        log.info("======== SERVER STARTED ========");
        log.info("\n====\n%s\n====", queryRunner.getCoordinator().getBaseUrl());
    }
}
