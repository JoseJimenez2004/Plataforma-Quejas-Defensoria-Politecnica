export declare function withModuleFederationPlugin(config: unknown): {
    plugins: any[];
    experiments?: {
        outputModule: boolean;
    };
    output: {
        publicPath: string;
    };
    optimization: {
        runtimeChunk: boolean;
    };
    resolve: {
        alias: {
            [x: string]: string;
        };
    };
};
