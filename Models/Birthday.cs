using System.Text;
using System.Runtime.Serialization;
using Newtonsoft.Json;

namespace Congratulator.Models
{
    [DataContract]
    public partial class Birthday : IEquatable<Birthday>
    {  
        [DataMember(Name="uuid")]
        public Guid? Uuid { get; set; }

        [DataMember(Name ="avatar")]
        public IFormFile Avatar {get; set;}

        [DataMember(Name="lastName")]
        public string LastName { get; set; }

        [DataMember(Name="firstName")]
        public string FirstName { get; set; }

        [DataMember(Name="middleName")]
        public string MiddleName { get; set; }

        [DataMember(Name="birthdayDate")]
        public DateTime? BirthdayDate { get; set; }

        public override string ToString()
        {
            var sb = new StringBuilder();
            sb.Append("class Birthday {\n");
            sb.Append("  Uuid: ").Append(Uuid).Append("\n");
            sb.Append("  Avatar: ").Append(Avatar).Append("\n");
            sb.Append("  LastName: ").Append(LastName).Append("\n");
            sb.Append("  FirstName: ").Append(FirstName).Append("\n");
            sb.Append("  MiddleName: ").Append(MiddleName).Append("\n");
            sb.Append("  BirthdayDate: ").Append(BirthdayDate).Append("\n");
            sb.Append("}\n");
            return sb.ToString();
        }

        public string ToJson()
        {
            return JsonConvert.SerializeObject(this, Formatting.Indented);
        }

        public override bool Equals(object obj)
        {
            if (ReferenceEquals(null, obj)) return false;
            if (ReferenceEquals(this, obj)) return true;
            return obj.GetType() == GetType() && Equals((Birthday)obj);
        }

        public bool Equals(Birthday other)
        {
            if (ReferenceEquals(null, other)) return false;
            if (ReferenceEquals(this, other)) return true;

            return 
                (
                    Uuid == other.Uuid ||
                    Uuid != null &&
                    Uuid.Equals(other.Uuid)
                ) && 
                (
                    Avatar == other.Avatar ||
                    Avatar != null &&
                    Avatar.Equals(other.Avatar)
                ) &&
                (
                    LastName == other.LastName ||
                    LastName != null &&
                    LastName.Equals(other.LastName)
                ) && 
                (
                    FirstName == other.FirstName ||
                    FirstName != null &&
                    FirstName.Equals(other.FirstName)
                ) && 
                (
                    MiddleName == other.MiddleName ||
                    MiddleName != null &&
                    MiddleName.Equals(other.MiddleName)
                ) && 
                (
                    BirthdayDate == other.BirthdayDate ||
                    BirthdayDate != null &&
                    BirthdayDate.Equals(other.BirthdayDate)
                );
        }

        public override int GetHashCode()
        {
            unchecked
            {
                var hashCode = 41;
                    if (Uuid != null)
                    hashCode = hashCode * 59 + Uuid.GetHashCode();
                    if (Avatar != null)
                    hashCode = hashCode * 59 + Avatar.GetHashCode();
                    if (LastName != null)
                    hashCode = hashCode * 59 + LastName.GetHashCode();
                    if (FirstName != null)
                    hashCode = hashCode * 59 + FirstName.GetHashCode();
                    if (MiddleName != null)
                    hashCode = hashCode * 59 + MiddleName.GetHashCode();
                    if (BirthdayDate != null)
                    hashCode = hashCode * 59 + BirthdayDate.GetHashCode();
                return hashCode;
            }
        }

        #region Operators
        #pragma warning disable 1591

        public static bool operator ==(Birthday left, Birthday right)
        {
            return Equals(left, right);
        }

        public static bool operator !=(Birthday left, Birthday right)
        {
            return !Equals(left, right);
        }

        #pragma warning restore 1591
        #endregion Operators
    }
}
