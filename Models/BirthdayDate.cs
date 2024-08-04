using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Congratulator.Models
{
    public class BirthdayDate
    {  
        [Key]
        [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
        public Guid? Uuid { get; set; }

        public DateTime? BirthdaysDate { get; set; }
    }
}