using Congratulator.Models;
using Microsoft.EntityFrameworkCore;
 
namespace HelloApp
{
    public class ApplicationContext : DbContext
    {
        public DbSet<Birthday> Users { get; set; }
        
        public ApplicationContext()
        {
            Database.EnsureCreated();
        }

        protected override void OnConfiguring(DbContextOptionsBuilder optionsBuilder)
        {
            optionsBuilder.UseNpgsql("Host=localhost;Port=5433;Database=congratulator;Username=pgUser;Password=very_d!ff!cu1t_p@ssw0rd");
        }
    }
}