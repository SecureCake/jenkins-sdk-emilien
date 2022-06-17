package com.couchbase.stages

import com.couchbase.fit.stages.BuildDockerGoSDKPerformer
import com.couchbase.fit.stages.BuildDockerPythonSDKPerformer
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import com.couchbase.context.StageContext
import com.couchbase.fit.perf.config.PerfConfig
import com.couchbase.fit.stages.BuildDockerJavaFITPerformer
import com.couchbase.fit.stages.BuildDockerJavaSDKPerformer
import com.couchbase.fit.stages.StartDockerImagePerformer

/**
 * Builds, copies (if needed), and runs a performer
 */
@CompileStatic
class InitialisePerformer extends Stage {
    private PerfConfig.Implementation impl
    private int port = 8060
    private String hostname

    InitialisePerformer(PerfConfig.Implementation impl) {
        this.impl = impl
        if (impl.port != null) {
            port = impl.port
        }
    }

    @Override
    String name() {
        return "Init performer $impl"
    }

    @CompileDynamic
    boolean skipDockerBuild(StageContext ctx) {
        return ctx.jc.settings.skipDockerBuild
    }

    @Override
    List<Stage> stagesPre(StageContext ctx) {
        if (impl.port != null) {
            // Nothing to do
            return []
        }
        else {
            if (impl.language == "java") {
                def stage1 = new BuildDockerJavaSDKPerformer(impl.version)
                return produceStages(ctx, stage1, stage1.getImageName())
            } else if (impl.language == "go"){
                def stage1 = new BuildDockerGoSDKPerformer(impl.version)
                return produceStages(ctx, stage1, stage1.getImageName())
            } else if (impl.language == "python"){
                def stage1 = new BuildDockerPythonSDKPerformer(impl.version)
                return produceStages(ctx, stage1, stage1.getImageName())
            } else{
                throw new IllegalArgumentException("Unknown performer ${impl.language}")
            }
        }
    }

    @Override
    void executeImpl(StageContext ctx) {}

    String hostname() {
        //return hostname
        //TODO change this
        return "performer"
    }

    int port() {
        return port
    }

    boolean isDocker() {
        return impl.port == null
    }

    List<Stage> produceStages(StageContext ctx,Stage stage1, String imageName){
        List<Stage> stages = []

        if (!skipDockerBuild(ctx)) {
            stages.add(stage1)
        }

        if (ctx.performerServer == "localhost") {
            stages.add(new StartDockerImagePerformer(imageName, port, impl.version))
        } else {
            throw new IllegalArgumentException("Cannot handle running on performer remote server")
        }

        return stages

    }
}