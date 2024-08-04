using Microsoft.OpenApi.Models;

public class Startup
{
    private readonly IWebHostEnvironment _hostingEnv;
    private IConfiguration Configuration { get; }

    public Startup(IWebHostEnvironment env, IConfiguration configuration)
    {
        _hostingEnv = env;
        Configuration = configuration;
    }

    public void ConfigureServices(IServiceCollection services)
    {
        services
            .AddSwaggerGen(c =>
            {
                c.SwaggerDoc("0.0.1", new OpenApiInfo
                {
                    Version = "0.0.1",
                    Title = "congratulator",
                    Description = "congratulator (ASP.NET Core 7.0)",
                    Contact = new OpenApiContact()
                    {
                       Name = "ASTiNEZh",
                       Url = new Uri("https://t.me/ASTiNEZh"),
                       Email = "astinezh@gmail.com"
                    },
                    TermsOfService = new Uri("")
                });
                c.CustomSchemaIds(type => type.FullName);
                c.IncludeXmlComments($"{AppContext.BaseDirectory}{Path.DirectorySeparatorChar}{_hostingEnv.ApplicationName}.xml");
            });
    }

    public void Configure(IApplicationBuilder app, IWebHostEnvironment env, ILoggerFactory loggerFactory)
    {
        app.UseRouting();
        app.UseAuthorization();
        app.UseSwagger();
        app.UseSwaggerUI(c =>
        {
            c.SwaggerEndpoint("/swagger/0.0.1/swagger.json", "congratulator");
        });
        app.UseEndpoints(endpoints =>
        {
            endpoints.MapControllers();
        });
        if (env.IsDevelopment())
        {
            app.UseDeveloperExceptionPage();
        }
        else
        {
            app.UseExceptionHandler("/Error");
            app.UseHsts();
        }
    }
}
