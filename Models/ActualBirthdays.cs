using System.Text;
using System.Runtime.Serialization;
using Newtonsoft.Json;

namespace Congratulator.Models
{
    [DataContract]
    public partial class ActualBirthdays : IEquatable<ActualBirthdays>
    { 
        [DataMember(Name="todayBirthdays")]
        public List<Birthday> TodayBirthdays { get; set; }

        [DataMember(Name="upcomingBirthdays")]
        public List<Birthday> UpcomingBirthdays { get; set; }

        public override string ToString()
        {
            var sb = new StringBuilder();
            sb.Append("class ActualBirthdays {\n");
            sb.Append("  TodayBirthdays: ").Append(TodayBirthdays).Append("\n");
            sb.Append("  UpcomingBirthdays: ").Append(UpcomingBirthdays).Append("\n");
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
            return obj.GetType() == GetType() && Equals((ActualBirthdays)obj);
        }

        public bool Equals(ActualBirthdays other)
        {
            if (ReferenceEquals(null, other)) return false;
            if (ReferenceEquals(this, other)) return true;

            return 
                (
                    TodayBirthdays == other.TodayBirthdays ||
                    TodayBirthdays != null &&
                    TodayBirthdays.SequenceEqual(other.TodayBirthdays)
                ) && 
                (
                    UpcomingBirthdays == other.UpcomingBirthdays ||
                    UpcomingBirthdays != null &&
                    UpcomingBirthdays.SequenceEqual(other.UpcomingBirthdays)
                );
        }

        public override int GetHashCode()
        {
            unchecked 
            {
                var hashCode = 41;
                    if (TodayBirthdays != null)
                    hashCode = hashCode * 59 + TodayBirthdays.GetHashCode();
                    if (UpcomingBirthdays != null)
                    hashCode = hashCode * 59 + UpcomingBirthdays.GetHashCode();
                return hashCode;
            }
        }

        #region Operators
        #pragma warning disable 1591

        public static bool operator ==(ActualBirthdays left, ActualBirthdays right)
        {
            return Equals(left, right);
        }

        public static bool operator !=(ActualBirthdays left, ActualBirthdays right)
        {
            return !Equals(left, right);
        }

        #pragma warning restore 1591
        #endregion Operators
    }
}
