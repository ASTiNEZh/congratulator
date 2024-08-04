using System.Text;
using System.Runtime.Serialization;
using Newtonsoft.Json;

namespace Congratulator.Models
{
    [DataContract]
    public partial class AllBirthdays : List<Birthday>, IEquatable<AllBirthdays>
    { 
        public override string ToString()
        {
            var sb = new StringBuilder();
            sb.Append("class AllBirthdays {\n");
            sb.Append("}\n");
            return sb.ToString();
        }

        public  new string ToJson()
        {
            return JsonConvert.SerializeObject(this, Formatting.Indented);
        }

        public override bool Equals(object obj)
        {
            if (ReferenceEquals(null, obj)) return false;
            if (ReferenceEquals(this, obj)) return true;
            return obj.GetType() == GetType() && Equals((AllBirthdays)obj);
        }

        public bool Equals(AllBirthdays other)
        {
            if (ReferenceEquals(null, other)) return false;
            if (ReferenceEquals(this, other)) return true;

            return false && base.Equals(other);
        }

        public override int GetHashCode()
        {
            unchecked
            {
                var hashCode = 41;
                return hashCode;
            }
        }

        #region Operators
        #pragma warning disable 1591

        public static bool operator ==(AllBirthdays left, AllBirthdays right)
        {
            return Equals(left, right);
        }

        public static bool operator !=(AllBirthdays left, AllBirthdays right)
        {
            return !Equals(left, right);
        }

        #pragma warning restore 1591
        #endregion Operators
    }
}
